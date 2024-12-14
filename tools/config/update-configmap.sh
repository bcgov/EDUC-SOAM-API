envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
PEN_NAMESPACE=$4
SPLUNK_TOKEN=$5

TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
SSO_ENV=loginproxy.gov.bc.ca
SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca
TARGET_ENV=$envValue

SOAM_KC_LOAD_USER_ADMIN=$(oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" -o json get secret sso-keycloak-admin | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" -o json get secret sso-keycloak-admin | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
DEVEXCHANGE_KC_CLIENT_ID=$(oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" -o json get secret standard-realm-creds-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
DEVEXCHANGE_KC_CLIENT_SECRET=$(oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" -o json get secret standard-realm-creds-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
DEVEXCHANGE_KC_REALM_ID="standard"
ENTRA_CLIENT_ID=$(oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" -o json get secret entra-creds-${envValue} | sed -n 's/.*"clientid": "\(.*\)",/\1/p' | base64 --decode)
ENTRA_CLIENT_SECRET=$(oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" -o json get secret entra-creds-${envValue} | sed -n 's/.*"secret": "\(.*\)"/\1/p' | base64 --decode)
SERVICES_CARD_DNS=id.gov.bc.ca

SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   HTTP_Port     2020
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
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
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"

if [ "$envValue" != "prod" ]; then
  SSO_ENV=$TARGET_ENV.loginproxy.gov.bc.ca
  SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca
  SERVICES_CARD_DNS=idtest.gov.bc.ca
fi


###########################################################
#Setup for SOAM SSO
###########################################################

echo Fetching SOAM token
TKN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=$SOAM_KC_LOAD_USER_ADMIN" \
  -d "password=$SOAM_KC_LOAD_USER_PASS" \
  -d "grant_type=password" \
  "https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" | jq -r '.access_token')

echo
echo Updating realm details
curl -sX PUT "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"loginWithEmailAllowed\" : false, \"duplicateEmailsAllowed\" : true, \"accessTokenLifespan\" : 300, \"loginTheme\": \"bcgov-v2\"}"

echo
echo Writing scope SOAM_LOGIN
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"SOAM login scope\",\"id\": \"SOAM_LOGIN\",\"name\": \"SOAM_LOGIN\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope SOAM_TENANT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"SOAM tenant scope\",\"id\": \"SOAM_TENANT\",\"name\": \"SOAM_TENANT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope SOAM_USER_INFO
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"SOAM user info scope\",\"id\": \"SOAM_USER_INFO\",\"name\": \"SOAM_USER_INFO\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope SCOPE_STS_ROLES
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"SOAM read sts roles scope\",\"id\": \"STS_ROLES\",\"name\": \"STS_ROLES\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope SCOPE_SOAM_LINK
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"SOAM read sts roles scope\",\"id\": \"SOAM_LINK\",\"name\": \"SOAM_LINK\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Retrieving client ID for soam-kc-service
soamKCClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq '.[] | select(.clientId=="soam-kc-service")' | jq -r '.id')

echo
echo Removing soam-kc-service client if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$soamKCClientID" \
  -H "Authorization: Bearer $TKN" \

echo
echo Creating client soam-kc-service
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"clientId\" : \"soam-kc-service\",\"name\" : \"SOAM Keycloak Service Account\",\"description\" : \"Client to call from SOAM KC to SOAM API\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {  \"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",  \"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { }, \"fullScopeAllowed\" : true, \"nodeReRegistrationTimeout\" : -1, \"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\", \"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\", \"protocol\" : \"openid-connect\", \"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,   \"config\" : {\"user.session.note\" : \"clientHost\", \"id.token.claim\" : \"true\", \"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"role_list\", \"profile\", \"roles\", \"SOAM_LOGIN\", \"SOAM_TENANT\",\"STS_ROLES\", \"email\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

echo
echo Retrieving client ID for soam-api-service
soamApiClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq '.[] | select(.clientId=="soam-api-service")' | jq -r '.id')

echo
echo Removing soam-api-service if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$soamApiClientID" \
  -H "Authorization: Bearer $TKN" \

echo
echo Creating client soam-api-service
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"clientId\" : \"soam-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"web-origins\", \"READ_SERVICES_CARD\", \"WRITE_SERVICES_CARD\", \"WRITE_STUDENT\", \"role_list\", \"READ_SERVICES_CARD\", \"WRITE_SERVICES_CARD\", \"READ_DIGITALID_CODETABLE\", \"WRITE_DIGITALID\", \"READ_STS\", \"profile\", \"roles\", \"READ_STUDENT\", \"email\", \"READ_DIGITALID\", \"READ_PEN_MATCH\" , \"READ_TENANT_ACCESS\" ],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"

echo
echo Retrieving client ID for soam-api-service
soamAPIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq '.[] | select(.clientId=="soam-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for soam-api-service
soamAPIServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$soamAPIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq -r '.value')

###########################################################
#Setup for config-map
###########################################################
echo
echo Creating config map $APP_NAME-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-api-config-map --from-literal=TZ=$TZVALUE --from-literal=CLIENT_ID=soam-api-service --from-literal=CLIENT_SECRET=$soamAPIServiceClientSecret --from-literal=DIGITALID_URL="http://digitalid-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/digital-id" --from-literal=STUDENT_URL="http://student-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student" --from-literal=PEN_MATCH_API_URL="http://pen-match-api-master.$PEN_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/pen-match" --from-literal=STS_API_URL="http://sts-api-main.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/sts" --from-literal=SERVICESCARD_API_URL="http://services-card-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/services-card" --from-literal=TOKEN_URL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=CIRCUITBREAKER_SLIDING_WINDOW_SIZE=100 --from-literal=CIRCUITBREAKER_CALLS_IN_HALF_OPEN=10 --from-literal=CIRCUITBREAKER_MINIMUM_CALLS=100 --from-literal=BULKHEAD_MAX_CONCURRENT_CALLS=25 --from-literal=RATELIMITER_LIMIT_FOR_PERIOD=50 --from-literal=RATELIMITER_LIMIT_REFRESH_PERIOD=500ns --from-literal=CIRCUITBREAKER_WAIT_DURATION_IN_OPEN=60000ms --from-literal=RETRY_MAX_ATTEMPTS=3 --from-literal=RATELIMITER_TIMEOUT_DURATION=5s --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc -n $OPENSHIFT_NAMESPACE-$envValue set env --from=configmap/$APP_NAME-config-map dc/$APP_NAME-$SOAM_KC_REALM_ID

echo Removing un-needed config entries
oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" set env dc/"$APP_NAME"-$SOAM_KC_REALM_ID KEYCLOAK_PUBLIC_KEY-

###########################################################
#Setup for soam-sso-config-map
###########################################################
#Authenticators-----------------------------------------------------------
echo
echo Creating authenticators
echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"alias\" : \"SOAMFirstLogin\",\"providerId\" : \"basic-flow\",\"topLevel\" : true,\"builtIn\" : false}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"alias\" : \"SOAMPostLogin\",\"providerId\" : \"basic-flow\",\"topLevel\" : true,\"builtIn\" : false}"

echo
echo Retrieving client ID for SOAM first login executor
soamFirstLoginExecutorID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows/SOAMFirstLogin/executions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq -r '.[].id')

echo
echo Retrieving client ID for SOAM post login executor
soamPostLoginExecutorID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows/SOAMPostLogin/executions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq -r '.[].id')

echo
echo Removing SOAM post login executor
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/executions/$soamPostLoginExecutorID" \
  -H "Authorization: Bearer $TKN" \

echo
echo Removing SOAM first login executor
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/executions/$soamFirstLoginExecutorID" \
  -H "Authorization: Bearer $TKN" \

echo
echo Creating SOAM executors
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows/SOAMPostLogin/executions/execution" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"provider\" : \"bcgov-soam-post-authenticator\"}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows/SOAMFirstLogin/executions/execution" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"provider\" : \"bcgov-soam-authenticator\"}"

echo
echo Retrieving client ID for SOAM first login executor
soamFirstLoginExecutorID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows/SOAMFirstLogin/executions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq -r '.[].id')

echo
echo Retrieving client ID for SOAM post login executor
soamPostLoginExecutorID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows/SOAMPostLogin/executions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq -r '.[].id')

echo
echo Updating SOAM first login executor to required
curl -sX PUT "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows/SOAMFirstLogin/executions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"id\": \"$soamFirstLoginExecutorID\", \"configurable\": false,\"displayName\": \"SOAM Authenticator\",\"index\": 0,\"level\": 0,\"providerId\": \"bcgov-soam-authenticator\",\"requirement\": \"REQUIRED\",\"requirementChoices\": [\"ALTERNATIVE\", \"REQUIRED\", \"DISABLED\"]}"

echo
echo Updating SOAM post login executor to required
curl -sX PUT "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/authentication/flows/SOAMPostLogin/executions" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"id\": \"$soamPostLoginExecutorID\", \"configurable\": false,\"displayName\": \"SOAM Authenticator\",\"index\": 0,\"level\": 0,\"providerId\": \"bcgov-soam-authenticator\",\"requirement\": \"REQUIRED\",\"requirementChoices\": [\"ALTERNATIVE\", \"REQUIRED\", \"DISABLED\"]}"

#Identity Providers------------------------------------------------

echo
echo Building IDP instance for BCeID...
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"alias\" : \"keycloak_bcdevexchange_bceid\",\"displayName\" : \"BCDevExchange Keycloak for BCeID Harry\",\"providerId\" : \"keycloak-oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"$DEVEXCHANGE_KC_CLIENT_ID\",\"tokenUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/certs\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/auth?kc_idp_hint=bceid\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$DEVEXCHANGE_KC_CLIENT_SECRET\",\"prompt\": \"\",\"defaultScope\" : \"openid\"}}"

echo
echo Creating mappers for IDP...
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bceid/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"identity_provider\",\"user.attribute\" : \"account_type\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bceid/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"bceid_guid\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"bceid_user_guid\",\"user.attribute\" : \"bceid_guid\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bceid/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"display_name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"display_name\",\"user.attribute\" : \"display_name\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bceid/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"user_guid\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"bceid_user_guid\",\"user.attribute\" : \"user_guid\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bceid/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"bceid_userid\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"bceid_username\",\"user.attribute\" : \"bceid_userid\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bceid/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"email\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bceid\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"email\",\"user.attribute\" : \"email\"}}"

echo
echo Building IDP instance for BCSC...
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"alias\" : \"keycloak_bcdevexchange_bcsc\",\"displayName\" : \"BCDevExchange Keycloak for BC Services Card\",\"providerId\" : \"oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"$bcscClientID\",\"tokenUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SERVICES_CARD_DNS/oauth2/\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SERVICES_CARD_DNS/oauth2/jwk\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SERVICES_CARD_DNS/login/oidc/authorize\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"\",\"clientSecret\" : \"$bcscClientSecret\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email address\"}}"

echo
echo Creating mappers for BC Services Card DevExchange IDP...
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"First Name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"firstName\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Email\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"email\",\"user.attribute\" : \"emailAddress\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Gender\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"gender\",\"user.attribute\" : \"gender\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"User Type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"user_type\",\"user.attribute\" : \"user_type\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"bcsc\",\"attribute\" : \"account_type\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Display Name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"display_name\",\"user.attribute\" : \"display_name\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Given Names\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_names\",\"user.attribute\" : \"given_names\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Given Name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"given_name\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Postal Code\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"address.postal_code\",\"user.attribute\" : \"postal_code\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Birthdate\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"birthdate\",\"user.attribute\" : \"birthdate\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Directed Identifier\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"sub\",\"user.attribute\" : \"bcsc_did\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"BCSC DID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"sub\",\"user.attribute\" : \"did\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"user_guid\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"sub\",\"user.attribute\" : \"user_guid\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Age\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"age\",\"user.attribute\" : \"age\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Identity Assurance Level\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"identity_assurance_level\",\"user.attribute\" : \"identity_assurance_level\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Last Name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"family_name\",\"user.attribute\" : \"family_name\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_bcsc/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"Username DID\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_bcsc\",\"identityProviderMapper\" : \"oidc-username-idp-mapper\",\"config\" : {\"template\" : \"\${CLAIM.sub}@\${ALIAS}\"}}"

echo
echo Building IDP instance for IDIR...
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"alias\" : \"keycloak_bcdevexchange_idir\",\"displayName\" : \"BCDevExchange Keycloak for IDIR Harry\",\"providerId\" : \"keycloak-oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"$DEVEXCHANGE_KC_CLIENT_ID\",\"tokenUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"issuer\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/certs\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/auth?kc_idp_hint=idir\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://$SSO_ENV/auth/realms/$DEVEXCHANGE_KC_REALM_ID/protocol/openid-connect/logout\",\"clientSecret\" : \"$DEVEXCHANGE_KC_CLIENT_SECRET\",\"prompt\": \"\",\"defaultScope\" : \"openid\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_idir/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"firstName\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"firstName\"}}"

curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_idir/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"lastName\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"family_name\",\"user.attribute\" : \"lastName\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_idir/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"identity_provider\",\"user.attribute\" : \"account_type\"}}"

curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_idir/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"email\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"email\",\"user.attribute\" : \"email\"}}"

echo Creating mappers for IDIR IDP...
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_idir/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"idir_username\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"idir_username\",\"user.attribute\" : \"idir_username\"}}"

curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_idir/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"display_name\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"display_name\",\"user.attribute\" : \"display_name\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_idir/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"idir_guid\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"idir_user_guid\",\"user.attribute\" : \"idir_guid\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/keycloak_bcdevexchange_idir/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"username\",\"identityProviderAlias\" : \"keycloak_bcdevexchange_idir\",\"identityProviderMapper\" : \"oidc-username-idp-mapper\",\"config\" : {\"template\" : \"\${CLAIM.idir_user_guid}\"}}"

echo
echo Building IDP instance for Entra...
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"alias\" : \"entra\",\"displayName\" : \"Entra\",\"providerId\" : \"oidc\",\"enabled\" : true,\"updateProfileFirstLoginMode\" : \"on\",\"firstBrokerLoginFlowAlias\" : \"SOAMFirstLogin\",\"postBrokerLoginFlowAlias\" : \"SOAMPostLogin\",\"trustEmail\" : false,\"storeToken\" : false,\"addReadTokenRoleOnCreate\" : false,\"authenticateByDefault\" : false,\"linkOnly\" : false,\"config\" : { \"hideOnLoginPage\" : \"true\",\"userInfoUrl\" : \"https://graph.microsoft.com/oidc/userinfo\",\"validateSignature\" : \"true\",\"clientId\" : \"$ENTRA_CLIENT_ID\",\"tokenUrl\" : \"https://login.microsoftonline.com/organizations/oauth2/v2.0/token\",\"uiLocales\" : \"\",\"backchannelSupported\" : \"\",\"useJwksUrl\" : \"true\",\"jwksUrl\" : \"https://login.microsoftonline.com/organizations/discovery/v2.0/keys\",\"loginHint\": \"\",\"authorizationUrl\" : \"https://login.microsoftonline.com/organizations/oauth2/v2.0/authorize\",\"disableUserInfo\" : \"\",\"logoutUrl\" : \"https://login.microsoftonline.com/organizations/oauth2/v2.0/logout\",\"clientSecret\" : \"$ENTRA_CLIENT_SECRET\",\"prompt\": \"\",\"defaultScope\" : \"openid profile email\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/entra/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"firstName\",\"identityProviderAlias\" : \"entra\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"given_name\",\"user.attribute\" : \"firstName\"}}"

curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/entra/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"lastName\",\"identityProviderAlias\" : \"entra\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"family_name\",\"user.attribute\" : \"lastName\"}}"

curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/entra/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"email\",\"identityProviderAlias\" : \"entra\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"email\",\"user.attribute\" : \"email\"}}"

curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/entra/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"tenant_id\",\"identityProviderAlias\" : \"entra\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"tid\",\"user.attribute\" : \"tenant_id\"}}"

curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/entra/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"entra_user_id\",\"identityProviderAlias\" : \"entra\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"oid\",\"user.attribute\" : \"entra_user_id\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/entra/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"username\",\"identityProviderAlias\" : \"entra\",\"identityProviderMapper\" : \"oidc-username-idp-mapper\",\"config\" : {\"template\" : \"\${CLAIM.oid}\"}}"

curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/entra/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"user_guid\",\"identityProviderAlias\" : \"entra\",\"identityProviderMapper\" : \"oidc-user-attribute-idp-mapper\",\"config\" : {\"claim\" : \"oid\",\"user.attribute\" : \"user_guid\"}}"

echo
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/identity-provider/instances/entra/mappers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"name\" : \"account_type\",\"identityProviderAlias\" : \"entra\",\"identityProviderMapper\" : \"hardcoded-attribute-idp-mapper\",\"config\" : {\"attribute.value\" : \"entra\",\"attribute\" : \"account_type\"}}"


# Retrieving client IDs and Secrets
echo
echo Retrieving client ID for soam-kc-service
soamKCServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq '.[] | select(.clientId=="soam-kc-service")' | jq -r '.id')

echo
echo Retrieving client secret for soam-api-service
soamKCServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$soamKCServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  | jq -r '.value')

echo
echo Creating config map soam-sso-config-map
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-sso-config-map --from-literal=TZ=$TZVALUE --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=clientID=soam-kc-service --from-literal=clientSecret=$soamKCServiceClientSecret --from-literal=soamApiURL="http://soam-api-master.$OPENSHIFT_NAMESPACE-$envValue.svc.cluster.local:8080" --from-literal=tokenURL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for sso-$envValue application
oc -n $OPENSHIFT_NAMESPACE-$envValue set env --from=configmap/soam-sso-config-map deployment/sso-keycloak

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -
