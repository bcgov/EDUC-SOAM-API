envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3

TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
KCADM_FILE_BIN_FOLDER="/tmp/keycloak-9.0.3/bin"
SSO_ENV=sso.pathfinder.gov.bc.ca
SOAM_KC=$OPENSHIFT_NAMESPACE.pathfinder.gov.bc.ca
NATS_CLUSTER=educ_pen_nats_cluster
NATS_URL="nats://nats.${OPENSHIFT_NAMESPACE}-${envValue}.svc.cluster.local:4222"

oc project $OPENSHIFT_NAMESPACE-$envValue
SOAM_KC_LOAD_USER_ADMIN=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)

if [ "$envValue" != "prod" ]
then
    SSO_ENV=sso-$envValue.pathfinder.gov.bc.ca
    SOAM_KC=$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca
fi

$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$SOAM_KC/auth --realm $SOAM_KC_REALM_ID --user $SOAM_KC_LOAD_USER_ADMIN --password $SOAM_KC_LOAD_USER_PASS

echo Updating realm details
$KCADM_FILE_BIN_FOLDER/kcadm.sh update realms/$SOAM_KC_REALM_ID --body "{\"loginWithEmailAllowed\" : false, \"duplicateEmailsAllowed\" : true, \"accessTokenLifespan\" : 1800}"

#SOAM_LOGIN
$KCADM_FILE_BIN_FOLDER/kcadm.sh create client-scopes -r $SOAM_KC_REALM_ID --body "{\"description\": \"SOAM login scope\",\"id\": \"SOAM_LOGIN\",\"name\": \"SOAM_LOGIN\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo Creating soam-kc-service client
$KCADM_FILE_BIN_FOLDER/kcadm.sh create clients -r $SOAM_KC_REALM_ID --body "{\"clientId\" : \"soam-kc-service\",\"name\" : \"SOAM Keycloak Service Account\",\"description\" : \"Client to call from SOAM KC to SOAM API\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {  \"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",  \"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { }, \"fullScopeAllowed\" : true, \"nodeReRegistrationTimeout\" : -1, \"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\", \"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,   \"config\" : {\"user.session.note\" : \"clientHost\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"profile\", \"roles\", \"SOAM_LOGIN\", \"email\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

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
newline=$'\n'
formattedPublicKey="${soamFullPublicKey:0:26}${newline}${soamFullPublicKey:27:64}${newline}${soamFullPublicKey:91:64}${newline}${soamFullPublicKey:155:64}${newline}${soamFullPublicKey:219:64}${newline}${soamFullPublicKey:283:64}${newline}${soamFullPublicKey:347:64}${newline}${soamFullPublicKey:411:9}${newline}${soamFullPublicKey:420}"

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
getSoamFirstLoginExecutorID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMFirstLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamFirstLoginExecutorID=$(getSoamFirstLoginExecutorID)

getSoamPostLoginExecutorID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get authentication/flows/SOAMPostLogin/executions -r $SOAM_KC_REALM_ID | grep -Po '"id" :(\d*?,|.*?[^\\]",)'
}

soamPostLoginExecutorID=$(getSoamPostLoginExecutorID)
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/executions/$soamPostLoginExecutorID -r $SOAM_KC_REALM_ID 
$KCADM_FILE_BIN_FOLDER/kcadm.sh create authentication/executions/$soamFirstLoginExecutorID -r $SOAM_KC_REALM_ID 

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
$KCADM_FILE_BIN_FOLDER/kcadm.sh create identity-provider/instances/keycloak_bcdevexchange_idir/mappers -r $SOAM_KC_REALM_ID --body "{\"name\" : \"IDIR GUID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"idir_userid\",\"user.attribute\" : \"idir_guid\"}}"

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
