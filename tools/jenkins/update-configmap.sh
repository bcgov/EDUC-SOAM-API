envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3

TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
KCADM_FILE_BIN_FOLDER="/home/jenkins/workspace/${OPENSHIFT_NAMESPACE}-tools/keycloak-9.0.3/bin"
SOAM_KC=$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca
NATS_CLUSTER=educ_pen_nats_cluster
NATS_URL="nats://nats.${OPENSHIFT_NAMESPACE}-${envValue}.svc.cluster.local:4222"

oc project $OPENSHIFT_NAMESPACE-$envValue
SOAM_KC_LOAD_USER_ADMIN=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
oc project $OPENSHIFT_NAMESPACE-tools

echo SOAM USER: $SOAM_KC_LOAD_USER_ADMIN
echo SOAM PASS: $SOAM_KC_LOAD_USER_PASS

###########################################################
#Fetch the public key
###########################################################
$KCADM_FILE_BIN_FOLDER/kcadm.sh config credentials --server https://$SOAM_KC/auth --realm $SOAM_KC_REALM_ID --user $SOAM_KC_LOAD_USER_ADMIN --password $SOAM_KC_LOAD_USER_PASS
getPublicKey(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get keys -r $SOAM_KC_REALM_ID | grep -Po 'publicKey" : "\K([^"]*)'
}

getSoamAPIServiceClientID(){
    executorID= $KCADM_FILE_BIN_FOLDER/kcadm.sh get clients -r $SOAM_KC_REALM_ID --fields 'id,clientId' | python3 -c "import sys, json; data = json.load(sys.stdin); output_dict = [x for x in data if x['clientId'] == 'soam-api-service'];  print(output_dict)" | grep -Po "(\{){0,1}[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}(\}){0,1}"
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
oc create -n $OPENSHIFT_NAMESPACE-$envValue configmap soam-api-config-map --from-literal=TZ=$TZVALUE --from-literal=CLIENT_ID=soam-api-service --from-literal=CLIENT_SECRET=$soamAPIServiceClientSecret --from-literal=DIGITALID_URL=https://digitalid-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=STUDENT_URL=https://student-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=SERVICESCARD_API_URL=https://services-card-api-$OPENSHIFT_NAMESPACE-$envValue.pathfinder.gov.bc.ca --from-literal=TOKEN_URL=https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token --from-literal=KEYCLOAK_PUBLIC_KEY="$soamFullPublicKey" --dry-run -o yaml | oc apply -f -
echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc project $OPENSHIFT_NAMESPACE-$envValue
oc set env --from=configmap/$APP_NAME-config-map dc/$APP_NAME-$SOAM_KC_REALM_ID