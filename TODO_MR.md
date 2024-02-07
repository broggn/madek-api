TODO_MR

Known issue
--
1. Broken tests (829 examples, 12 failures)
   1. ./spec/resources/meta-data/meta-datum-keywords_spec.rb
   2. Its a known issue, see: https://github.com/Madek/madek-datalayer/blob/master/spec/models/keyword/terms_for_sorting_shared_context.rb#L3-L4
   3. FYI: test deactivated for the moment
   4. Endpoints that fails:
      ```bash
      "/api/meta-data/#{meta_datum_keywords.id}"
      "/api/meta-keys/#{meta_key_id}"
      "/api/media-entry/#{media_entry_id}"
      "/api/collection/#{collection_id}"
      ```  


ToAsk
--
1. How are `auth-enttity/is_admin` use?
   1. https://github.com/Madek/madek-api-v2/blob/master/src/madek/api/authentication.clj#L10-L26
   2. https://github.com/Madek/madek-api-v2/blob/master/src/madek/api/utils/auth.clj#L10-L35

FYI
--
1. Added a headers-parsing function for headers
   1. headers.id & headers.is_admin to be 
   2. headers.id & headers.is_admin to be 


