import json

json_file='./google-services.json'
index=0

json_data=open(json_file)
data = json.load(json_data)
json_data.close()


print "To add environment variables, paste below to your bash profile and restart your IDE\n"
print "---------------------------"
print "export default_web_client_id="+data['client'][index]['oauth_client'][index]['client_id']
print "export firebase_database_url="+ data['project_info']['firebase_url']
print "export gcm_defaultSenderId="+ data['project_info']['project_number']
print "export google_api_key="+data['client'][index]['api_key'][index]['current_key']
print "export google_app_id="+data['client'][index]['client_info']['mobilesdk_app_id']
print "export google_crash_reporting_api_key="+data['client'][index]['api_key'][index]['current_key']
print "export project_id="+ data['project_info']['project_id']
print "---------------------------\n"