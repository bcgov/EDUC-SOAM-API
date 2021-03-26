envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
DEVEXCHANGE_KC_REALM_ID=$4
APP_NAME_UPPER=${APP_NAME^^}

TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
KCADM_FILE_BIN_FOLDER="/tmp/keycloak-9.0.3/bin"
SSO_ENV=oidc.gov.bc.ca
SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca
TARGET_ENV=$envValue

#THIS CONDITION IS ONLY ADDED FOR LOWER ENV DEPLOYMENT
if [ "$envValue" == "tools" ]; then
  TARGET_ENV="dev"
fi

if [ "$envValue" == "dev" ]; then
  TARGET_ENV="test"
fi

oc project "$OPENSHIFT_NAMESPACE"-"$envValue"
SOAM_KC_LOAD_USER_ADMIN=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
DEVEXCHANGE_KC_LOAD_USER_PASS=$(oc -o json get secret devexchange-keycloak-secrets-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
DEVEXCHANGE_KC_LOAD_USER_ADMIN=$(oc -o json get secret devexchange-keycloak-secrets-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
DEVEXCHANGE_KC_REALM_ID=$(oc -o json get secret devexchange-keycloak-secrets-${envValue} | sed -n 's/.*"realm": "\(.*\)",/\1/p' | base64 --decode)
SPLUNK_TOKEN=$(oc -o json get configmaps ${APP_NAME}-${envValue}-setup-config | sed -n "s/.*\"SPLUNK_TOKEN_${APP_NAME_UPPER}\": \"\(.*\)\"/\1/p")
SERVICES_CARD_DNS=id.gov.bc.ca

SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
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

if [ "$envValue" != "prod" ]; then
  SSO_ENV=$TARGET_ENV.oidc.gov.bc.ca
  SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca
  SERVICES_CARD_DNS=idtest.gov.bc.ca
fi
###########################################################
#Setup for Dev Exchange
###########################################################
echo Logging in
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://"$SSO_ENV"/auth --realm "$DEVEXCHANGE_KC_REALM_ID" --user "$DEVEXCHANGE_KC_LOAD_USER_ADMIN" --password "$DEVEXCHANGE_KC_LOAD_USER_PASS"

getSoamClientID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $DEVEXCHANGE_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getSoamClientID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $DEVEXCHANGE_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
getSoamClientSecret() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$soamClientID/client-secret -r $DEVEXCHANGE_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

soamClientID=$(getSoamClientID)
soamClientSecret=$(getSoamClientSecret)

###########################################################
#Setup for SOAM SSO
###########################################################
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$SOAM_KC/auth --realm $SOAM_KC_REALM_ID --user $SOAM_KC_LOAD_USER_ADMIN --password $SOAM_KC_LOAD_USER_PASS

echo Updating realm details
$KCADM_FILE_BIN_FOLDER/kcadm.sh update realms/$SOAM_KC_REALM_ID --body "{\"loginWithEmailAllowed\" : false, \"duplicateEmailsAllowed\" : true, \"accessTokenLifespan\" : 1800, \"loginTheme\": \"bcgov-v2\"}"

#SOAM_LOGIN
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM login scope\",\"id\": \"SOAM_LOGIN\",\"name\": \"SOAM_LOGIN\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

getSoamKCClientID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam-kc-service"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
soamKCClientID=$(getSoamKCClientID)

echo Removing SOAM KC client if exists
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete clients/$soamKCClientID -r $SOAM_KC_REALM_ID

echo Creating soam-kc-service client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"soam-kc-service\",\"name\" : \"SOAM Keycloak Service Account\",\"description\" : \"Client to call from SOAM KC to SOAM API\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {  \"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",  \"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { }, \"fullScopeAllowed\" : true, \"nodeReRegistrationTimeout\" : -1, \"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\", \"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,   \"config\" : {\"user.session.note\" : \"clientHost\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"profile\", \"roles\", \"SOAM_LOGIN\", \"email\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

getSoamApiClientID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam-api-service"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
soamApiClientID=$(getSoamApiClientID)

echo Removing SOAM API client if exists
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete clients/$soamApiClientID -r $SOAM_KC_REALM_ID

echo Creating soam-api-service Keycloak client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"soam-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"READ_SERVICES_CARD\", \"WRITE_SERVICES_CARD\", \"WRITE_STUDENT\", \"role_list\", \"READ_SERVICES_CARD\", \"WRITE_SERVICES_CARD\", \"READ_DIGITALID_CODETABLE\", \"WRITE_DIGITALID\", \"profile\", \"roles\", \"READ_STUDENT\", \"email\", \"READ_DIGITALID\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

getPublicKey() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get keys -r $SOAM_KC_REALM_ID | grep -Po 'publicKey" : "\K([^"]*)'
}

getSoamAPIServiceClientID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam-api-service"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}
getSoamAPIServiceClientSecret() {
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
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-api-config-map --from-literal=TZ=$TZVALUE --from-literal=CLIENT_ID=soam-api-service --from-literal=CLIENT_SECRET=$soamAPIServiceClientSecret --from-literal=DIGITALID_URL="http://digitalid-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080" --from-literal=STUDENT_URL="http://student-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student" --from-literal=SERVICESCARD_API_URL="http://services-card-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080" --from-literal=TOKEN_URL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc set env --from=configmap/$APP_NAME-config-map dc/$APP_NAME-$SOAM_KC_REALM_ID

###########################################################
#Setup for soam-sso-config-map
###########################################################
#Authenticators-----------------------------------------------------------
echo Creating authenticators
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"SOAMFirstLogin\",\"providerId\" : \"basic-flow\",\"topLevel\" : true,\"builtIn\" : false}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"SOAMPostLogin\",\"providerId\" : \"basic-flow\",\"topLevel\" : true,\"builtIn\" : false}"

echo Removing executors if exists
getSoamFirstLoginExecutorIDForDelete() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

soamFirstLoginExecutorID=$(getSoamFirstLoginExecutorIDForDelete)

getSoamPostLoginExecutorIDForDelete() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

soamPostLoginExecutorID=$(getSoamPostLoginExecutorIDForDelete)
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete authentication/executions/$soamPostLoginExecutorID -r $SOAM_KC_REALM_ID
$KCADM_FILE_BIN_FOLDER/kcadm.sh delete authentication/executions/$soamFirstLoginExecutorID -r $SOAM_KC_REALM_ID

echo Creating executors
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows/SOAMPostLogin/executions/execution -r $SOAM_KC_REALM_ID -s provider=bcgov-soam-post-authenticator
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/flows/SOAMFirstLogin/executions/execution -r $SOAM_KC_REALM_ID -s provider=bcgov-soam-authenticator

getSoamFirstLoginExecutorID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamFirstLoginExecutorID=$(getSoamFirstLoginExecutorID)

getSoamPostLoginExecutorID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamPostLoginExecutorID=$(getSoamPostLoginExecutorID)

echo Updating first login executor to required
$KCADM_FILE_BIN_FOLDER/kcadm.sh update authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID --body "{$soamFirstLoginExecutorID \"configurable\": false,\"displayName\": \"SOAM Authenticator\",\"index\": 0,\"level\": 0,\"providerId\": \"bcgov-soam-authenticator\",\"requirement\": \"REQUIRED\",\"requirementChoices\": [\"ALTERNATIVE\", \"REQUIRED\", \"DISABLED\"]}"

getSoamPostLoginExecutorID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamPostLoginExecutorID=$(getSoamPostLoginExecutorID)
echo Updating post login executor to required
$KCADM_FILE_BIN_FOLDER/kcadm.sh update authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID --body "{$soamPostLoginExecutorID \"configurable\": false,\"displayName\": \"SOAM Authenticator\",\"index\": 0,\"level\": 0,\"providerId\": \"bcgov-soam-authenticator\",\"requirement\": \"REQUIRED\",\"requirementChoices\": [\"ALTERNATIVE\", \"REQUIRED\", \"DISABLED\"]}"

#Identity Providers------------------------------------------------

echo Creating DevExchange IDP for BCeID
echo Building IDP instance for BCeID...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"keycloak_bcdevexchange_bceid\",\"displayName\" : \"BCDevExchange Keycloak for BCeID\",\"providerId\" : \"keycloak-oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"soam\",\"tokenUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/certs\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/auth?kc_idp_hint=bceid\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$soamClientSecret\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email address\"}}"
echo Creating mappers for IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bceid/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"account_type\",\"user.attribute\" : \"account_type\"}}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bceid/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"BCeID GUID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"bceid_userid\",\"user.attribute\" : \"bceid_guid\"}}"

echo Creating DevExchange IDP for BCSC
echo Building IDP instance for BCSC...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"keycloak_bcdevexchange_bcsc\",\"displayName\" : \"BCDevExchange Keycloak for BC Services Card\",\"providerId\" : \"oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"$bcscClientID\",\"tokenUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SERVICES_CARD_DNS/oauth2/\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/jwk.json\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SERVICES_CARD_DNS/login/oidc/authorize\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$bcscClientSecret\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email address\"}}"

echo Creating mappers for BC Services Card DevExchange IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"First Name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"firstName\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Email\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"email\",\"user.attribute\" : \"emailAddress\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Gender\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"gender\",\"user.attribute\" : \"gender\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"User Type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"user_type\",\"user.attribute\" : \"user_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"bcsc\",\"attribute\" : \"account_type\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Display Name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"display_name\",\"user.attribute\" : \"display_name\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Region\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.region\",\"user.attribute\" : \"region\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Given Names\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_names\",\"user.attribute\" : \"given_names\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Given Name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"given_name\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Street Address\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.street_address\",\"user.attribute\" : \"street_address\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Postal Code\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.postal_code\",\"user.attribute\" : \"postal_code\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Country\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.country\",\"user.attribute\" : \"country\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Birthdate\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"birthdate\",\"user.attribute\" : \"birthdate\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Locality\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.locality\",\"user.attribute\" : \"locality\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Directed Identifier\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"sub\",\"user.attribute\" : \"bcsc_did\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Age\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"age\",\"user.attribute\" : \"age\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Identity Assurance Level\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"identity_assurance_level\",\"user.attribute\" : \"identity_assurance_level\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Last Name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"family_name\",\"user.attribute\" : \"family_name\"}}"

$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"Username DID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-username-idp-mapper\",\"config\" : {\"template\" : \"\${CLAIM.sub}@\${ALIAS}\"}}"

echo Creating DevExchange IDP for IDIR
echo Building IDP instance for IDIR...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances -r $SOAM_KC_REALM_ID --body "{\"alias\" : \"keycloak_bcdevexchange_idir\",\"displayName\" : \"BCDevExchange Keycloak for IDIR\",\"providerId\" : \"keycloak-oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"soam\",\"tokenUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/certs\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/auth?kc_idp_hint=idir\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$soamClientSecret\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email address\"}}"
echo Creating mappers for IDP...
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_idir/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"account_type\",\"user.attribute\" : \"account_type\"}}"
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_idir/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"IDIR GUID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"idir_guid\",\"user.attribute\" : \"idir_guid\"}}"

getSoamKCServiceClientID() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | grep -B2 '"clientId" : "soam-kc-service"' | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

getSoamKCServiceClientSecret() {
  executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients/$soamKCServiceClientID/client-secret -r $SOAM_KC_REALM_ID | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
}

echo Fetching client ID for soam-kc-service client
soamKCServiceClientID=$(getSoamKCServiceClientID)
echo Fetching client secret for soam-kc-service client
soamKCServiceClientSecret=$(getSoamKCServiceClientSecret)

echo Creating config map soam-sso-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-sso-config-map --from-literal=TZ=$TZVALUE --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=clientID=soam-kc-service --from-literal=clientSecret=$soamKCServiceClientSecret --from-literal=soamApiURL="http://soam-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080" --from-literal=tokenURL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for sso-$envValue application
oc set env --from=configmap/soam-sso-config-map dc/sso-$envValue

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --dry-run -o yaml | oc apply -f -
