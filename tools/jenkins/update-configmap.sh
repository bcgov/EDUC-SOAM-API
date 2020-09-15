envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
DEVEXCHANGE_KC_REALM_ID=$4
APP_NAME_UPPER=${APP_NAME^^}

TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
KCADM_FILE_BIN_FOLDER="/mnt/c/Arcshift/Apps/keycloak-8.0.1/bin"
SSO_ENV=sso.pathfinder.gov.bc.ca
SOAM_KC=$OPENSHIFT_NAMESPACE.pathfinder.gov.bc.ca

oc project "$OPENSHIFT_NAMESPACE"-"$envValue"
SOAM_KC_LOAD_USER_ADMIN=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
DEVEXCHANGE_KC_LOAD_USER_PASS=$(oc -o json get secret devexchange-keycloak-secrets-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
DEVEXCHANGE_KC_LOAD_USER_ADMIN=$(oc -o json get secret devexchange-keycloak-secrets-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
DEVEXCHANGE_KC_REALM_ID=$(oc -o json get secret devexchange-keycloak-secrets-${envValue} | sed -n 's/.*"realm": "\(.*\)",/\1/p' | base64 --decode)
SPLUNK_TOKEN=$(oc -o json get configmaps ${APP_NAME}-${envValue}-setup-config | sed -n "s/.*\"SPLUNK_TOKEN_${APP_NAME_UPPER}\": \"\(.*\)\"/\1/p")
SERVICES_CARD_DNS=id.gov.bc.ca

SPLUNK_URL=""
if [ "$envValue" != "prod" ]
then
  SSO_ENV=sso-$envValue.pathfinder.gov.bc.ca
  SOAM_KC=$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca
  SERVICES_CARD_DNS=idtest.gov.bc.ca
  SPLUNK_URL="dev.splunk.educ.gov.bc.ca"
  FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   HTTP_Port     2020
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Mem_Buf_Limit 20MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
else
  FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   HTTP_Port     2020
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Mem_Buf_Limit 20MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
"
fi
###########################################################
#Setup for Dev Exchange
###########################################################
echo Logging in
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://"$SSO_ENV"/auth --realm "$DEVEXCHANGE_KC_REALM_ID" --user "$DEVEXCHANGE_KC_LOAD_USER_ADMIN" --password "$DEVEXCHANGE_KC_LOAD_USER_PASS"

echo Updating realm details
$KCADM_FILE_BIN_FOLDER/kcadm.sh update realms/"$DEVEXCHANGE_KC_REALM_ID" --body "{\"loginWithEmailAllowed\" : false, \"duplicateEmailsAllowed\" : true}"

echo Updating First Broker Login executers
getFirstBrokerLoginRegistrationExecuterID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get -r "$DEVEXCHANGE_KC_REALM_ID" authentication/flows/first%20broker%20login/executions | grep -B6 '"providerId" : "idp-review-profile"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

FIRST_BROKER_EXECUTER_ID=$(getFirstBrokerLoginRegistrationExecuterID)
$KCADM_FILE_BIN_FOLDER/kcadm.sh update authentication/flows/first%20broker%20login/executions -r "$DEVEXCHANGE_KC_REALM_ID" --body "{ \"id\" : \"$FIRST_BROKER_EXECUTER_ID\", \"requirement\" : \"DISABLED\", \"displayName\" : \"Review Profile\", \"alias\" : \"review profile config\", \"requirementChoices\" : [ \"REQUIRED\", \"DISABLED\" ], \"configurable\" : true, \"providerId\" : \"idp-review-profile\", \"authenticationConfig\" : \"0ee684cd-7ce1-4278-9477-d40d1a3486bf\", \"level\" : 0, \"index\" : 0}"

echo Removing BCSC IDP if exists...
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete identity-provider/instances/bcsc -r "$DEVEXCHANGE_KC_REALM_ID"

echo Creating BC Services Card IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r "$DEVEXCHANGE_KC_REALM_ID" --body "{\"alias\" : \"bcsc\",\"displayName\" : \"BC Services Card\",\"providerId\" : \"oidc\",\"enabled\" : false,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"first broker login\",\"config\" : {\"hideOnLoginPage\" : \"\",\"userInfoUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"$bcscClientID\",\"tokenUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/token\",\"uiLocales\" : \"\",\"jwksUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/jwk.json\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SERVICES_CARD_DNS/oauth2/\",\"useJwksUrl\" : \"true\",\"loginHint\" : \"\",\"authorizationUrl\" : \"https://$SERVICES_CARD_DNS/login/oidc/authorize\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"\",\"clientSecret\" : \"$bcscClientSecret\",\"prompt\" : \"\",\"defaultScope\" : \"openid profile email address\"}}"

echo Creating mappers for BC Services Card DevExchange IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"First Name\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"firstName\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Email\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"email\",\"user.attribute\" : \"email\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Gender\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"gender\",\"user.attribute\" : \"gender\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"User Type\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"user_type\",\"user.attribute\" : \"user_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"bcsc\",\"attribute\" : \"account_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Display Name\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"display_name\",\"user.attribute\" : \"display_name\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Region\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.region\",\"user.attribute\" : \"region\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Given Names\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_names\",\"user.attribute\" : \"given_names\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Given Name\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"given_name\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Street Address\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.street_address\",\"user.attribute\" : \"street_address\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Postal Code\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.postal_code\",\"user.attribute\" : \"postal_code\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Country\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.country\",\"user.attribute\" : \"country\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Birthdate\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"birthdate\",\"user.attribute\" : \"birthdate\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Locality\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.locality\",\"user.attribute\" : \"locality\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Directed Identifier\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"sub\",\"user.attribute\" : \"did\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Age\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"age\",\"user.attribute\" : \"age\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Identity Assurance Level\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"identity_assurance_level\",\"user.attribute\" : \"identity_assurance_level\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Last Name\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"family_name\",\"user.attribute\" : \"lastName\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bcsc/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"Username DID\",\"identityProviderAlias\" : \"bcsc\",\"identityProviderMapper\" : \"oidc-username-idp-mapper\",\"config\" : {\"template\" : \"\${CLAIM.sub}@\${ALIAS}\"}}"

echo Creating mappers for IDIR DevExchange IDP if not exist... 

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/idir/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"idir\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"idir\",\"attribute\" : \"account_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/idir/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"idir_userid\",\"identityProviderAlias\" : \"idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"idir_userid\",\"user.attribute\" : \"idir_userid\"}}"

echo Creating mappers for BCeID DevExchange IDP if not exist...

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bceid/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"bceid\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"bceid\",\"attribute\" : \"account_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/bceid/mappers -r $DEVEXCHANGE_KC_REALM_ID --body "{\"name\" : \"bceid_userid\",\"identityProviderAlias\" : \"bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"bceid_userid\",\"user.attribute\" : \"bceid_userid\"}}"

getSoamClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $DEVEXCHANGE_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
soamClientID=$(getSoamClientID)

echo Removing SOAM client if exists
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete clients/$soamClientID -r $DEVEXCHANGE_KC_REALM_ID

echo Creating SOAM client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $DEVEXCHANGE_KC_REALM_ID --body "{\"clientId\" : \"soam\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_bceid/endpoint/logout_response\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_bceid/endpoint\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_idir/endpoint\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_idir/endpoint/logout_response\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_bcsc/endpoint\", \"https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/broker/keycloak_bcdevexchange_bcsc/endpoint/logout_response\" ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : true,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : false,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"IDIR GUID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"idir_userid\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"idir_guid\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"display_name\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"displayName\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"display_name\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"BCSC DID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"did\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"bcsc_did\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"BCeID GUID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"bceid_userid\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"bceid_guid\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Identity Assurance Level\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"identity_assurance_level\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"identity_assurance_level\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Region\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"region\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"region\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Locality\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"locality\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"locality\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Street Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"street_address\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"street_address\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Country\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"country\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"country\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Postal Code\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"postal_code\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"postal_code\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"username\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-property-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"username\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"preferred_username\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Account Type\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"account_type\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"account_type\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Given Names\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usermodel-attribute-mapper\",\"consentRequired\" : false,\"config\" : {\"userinfo.token.claim\" : \"true\",\"user.attribute\" : \"given_names\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"given_names\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

getSoamClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $DEVEXCHANGE_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
getSoamClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$soamClientID/client-secret -r $DEVEXCHANGE_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

soamClientID=$(getSoamClientID)
soamClientSecret=$(getSoamClientSecret)

###########################################################
#Setup for SOAM SSO
###########################################################
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$SOAM_KC/auth --realm $SOAM_KC_REALM_ID --user $SOAM_KC_LOAD_USER_ADMIN --password $SOAM_KC_LOAD_USER_PASS

echo Updating realm details
$KCADM_FILE_BIN_FOLDER/kcadm.sh update realms/$SOAM_KC_REALM_ID --body "{\"loginWithEmailAllowed\" : false, \"duplicateEmailsAllowed\" : true, \"accessTokenLifespan\" : 1800}"

#SOAM_LOGIN
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM login scope\",\"id\": \"SOAM_LOGIN\",\"name\": \"SOAM_LOGIN\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

getSoamKCClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam-kc-service"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
soamKCClientID=$(getSoamKCClientID)

echo Removing SOAM KC client if exists
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete clients/$soamKCClientID -r $SOAM_KC_REALM_ID

echo Creating soam-kc-service client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"soam-kc-service\",\"name\" : \"SOAM Keycloak Service Account\",\"description\" : \"Client to call from SOAM KC to SOAM API\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {  \"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",  \"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { }, \"fullScopeAllowed\" : true, \"nodeReRegistrationTimeout\" : -1, \"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\", \"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,   \"config\" : {\"user.session.note\" : \"clientHost\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"profile\", \"roles\", \"SOAM_LOGIN\", \"email\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

getSoamApiClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam-api-service"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
soamApiClientID=$(getSoamApiClientID)

echo Removing SOAM API client if exists
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete clients/$soamApiClientID -r $SOAM_KC_REALM_ID

echo Creating soam-api-service Keycloak client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"soam-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"READ_SERVICES_CARD\", \"WRITE_SERVICES_CARD\", \"WRITE_STUDENT\", \"role_list\", \"READ_SERVICES_CARD\", \"WRITE_SERVICES_CARD\", \"READ_DIGITALID_CODETABLE\", \"WRITE_DIGITALID\", \"profile\", \"roles\", \"READ_STUDENT\", \"email\", \"READ_DIGITALID\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"


getPublicKey(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get keys -r $SOAM_KC_REALM_ID | grep -Po 'publicKey" : "\K([^"]*)'
}

getSoamAPIServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam-api-service"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
getSoamAPIServiceClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$soamAPIServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
soamAPIServiceClientID=$(getSoamAPIServiceClientID)
soamAPIServiceClientSecret=$(getSoamAPIServiceClientSecret)

echo Fetching public key from SOAM
soamFullPublicKey="-----BEGIN PUBLIC KEY----- $(getPublicKey) -----END PUBLIC KEY-----"

###########################################################
#Setup for config-map
###########################################################
echo
echo Creating config map $APP_NAME-config-map 
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-api-config-map --from-literal=TZ=$TZVALUE --from-literal=CLIENT_ID=soam-api-service --from-literal=CLIENT_SECRET=$soamAPIServiceClientSecret --from-literal=DIGITALID_URL=https://digitalid-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=STUDENT_URL=https://student-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=SERVICESCARD_API_URL=https://services-card-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=TOKEN_URL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc set env --from=configmap/$APP_NAME-config-map dc/$APP_NAME-$SOAM_KC_REALM_ID

###########################################################
#Setup for soam-sso-config-map
###########################################################
#Authenticators-----------------------------------------------------------
echo Creating authenticators
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows -r $SOAM_KC_REALM_ID  --body "{\"alias\" : \"SOAMFirstLogin\",\"providerId\" : \"basic-flow\",\"topLevel\" : true,\"builtIn\" : false}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows -r $SOAM_KC_REALM_ID  --body "{\"alias\" : \"SOAMPostLogin\",\"providerId\" : \"basic-flow\",\"topLevel\" : true,\"builtIn\" : false}"

echo Removing executors if exists
getSoamFirstLoginExecutorIDForDelete(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

soamFirstLoginExecutorID=$(getSoamFirstLoginExecutorIDForDelete)

getSoamPostLoginExecutorIDForDelete(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

soamPostLoginExecutorID=$(getSoamPostLoginExecutorIDForDelete)
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete authentication/executions/$soamPostLoginExecutorID -r $SOAM_KC_REALM_ID 
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete authentication/executions/$soamFirstLoginExecutorID -r $SOAM_KC_REALM_ID 

echo Creating executors
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows/SOAMPostLogin/executions/execution -r $SOAM_KC_REALM_ID -s provider=bcgov-soam-post-authenticator
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows/SOAMFirstLogin/executions/execution -r $SOAM_KC_REALM_ID -s provider=bcgov-soam-authenticator

getSoamFirstLoginExecutorID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)' 
}

soamFirstLoginExecutorID=$(getSoamFirstLoginExecutorID)

getSoamPostLoginExecutorID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamPostLoginExecutorID=$(getSoamPostLoginExecutorID)

echo Updating first login executor to required
$KCADM_FILE_BIN_FOLDER/kcadm.sh update authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID  --body "{$soamFirstLoginExecutorID \"configurable\": false,\"displayName\": \"SOAM Authenticator\",\"index\": 0,\"level\": 0,\"providerId\": \"bcgov-soam-authenticator\",\"requirement\": \"REQUIRED\",\"requirementChoices\": [\"ALTERNATIVE\", \"REQUIRED\", \"DISABLED\"]}"

getSoamPostLoginExecutorID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamPostLoginExecutorID=$(getSoamPostLoginExecutorID)
echo Updating post login executor to required
$KCADM_FILE_BIN_FOLDER/kcadm.sh update authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID  --body "{$soamPostLoginExecutorID \"configurable\": false,\"displayName\": \"SOAM Authenticator\",\"index\": 0,\"level\": 0,\"providerId\": \"bcgov-soam-authenticator\",\"requirement\": \"REQUIRED\",\"requirementChoices\": [\"ALTERNATIVE\", \"REQUIRED\", \"DISABLED\"]}"

#Identity Providers------------------------------------------------
echo Removing BCSC IDP if exists...
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete identity-provider/instances/keycloak_bcdevexchange_bcsc -r $SOAM_KC_REALM_ID

echo Removing BCeID IDP if exists...
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete identity-provider/instances/keycloak_bcdevexchange_bceid -r $SOAM_KC_REALM_ID

echo Removing IDIR IDP if exists...
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete identity-provider/instances/keycloak_bcdevexchange_idir -r $SOAM_KC_REALM_ID

echo Creating DevExchange IDP for BCeID
echo Building IDP instance for BCeID...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"keycloak_bcdevexchange_bceid\",\"displayName\" : \"BCDevExchange Keycloak for BCeID\",\"providerId\" : \"keycloak-oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"soam\",\"tokenUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/certs\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/auth?kc_idp_hint=bceid\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$soamClientSecret\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email address\"}}"
echo Creating mappers for IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bceid/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"account_type\",\"user.attribute\" : \"account_type\"}}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bceid/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"BCeID GUID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"bceid_userid\",\"user.attribute\" : \"bceid_guid\"}}"
 
echo Creating DevExchange IDP for BCSC
echo Building IDP instance for BCSC...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"keycloak_bcdevexchange_bcsc\",\"displayName\" : \"BCDevExchange Keycloak for BC Services Card\",\"providerId\" : \"keycloak-oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"soam\",\"tokenUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/certs\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/auth?kc_idp_hint=bcsc\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$soamClientSecret\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email address\"}}"
echo Creating mappers for IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"account_type\",\"user.attribute\" : \"account_type\"}}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"BCSC DID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"did\",\"user.attribute\" : \"bcsc_did\"}}"

echo Creating DevExchange IDP for IDIR
echo Building IDP instance for IDIR...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"keycloak_bcdevexchange_idir\",\"displayName\" : \"BCDevExchange Keycloak for IDIR\",\"providerId\" : \"keycloak-oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"soam\",\"tokenUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/certs\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/auth?kc_idp_hint=idir\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$soamClientSecret\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email address\"}}"
echo Creating mappers for IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_idir/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"account_type\",\"user.attribute\" : \"account_type\"}}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_idir/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"IDIR GUID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"idir_guid\",\"user.attribute\" : \"idir_guid\"}}"

getSoamKCServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam-kc-service"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getSoamKCServiceClientSecret(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$soamKCServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

echo Fetching client ID for soam-kc-service client
soamKCServiceClientID=$(getSoamKCServiceClientID)
echo Fetching client secret for soam-kc-service client
soamKCServiceClientSecret=$(getSoamKCServiceClientSecret)

echo Creating config map soam-sso-config-map 
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-sso-config-map --from-literal=TZ=$TZVALUE --from-literal=clientID=soam-kc-service --from-literal=clientSecret=$soamKCServiceClientSecret --from-literal=soamApiURL=https://soam-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=tokenURL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for sso-$envValue application
oc set env --from=configmap/soam-sso-config-map dc/sso-$envValue

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG"  --dry-run -o yaml | oc apply -f -
