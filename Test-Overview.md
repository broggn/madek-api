
TODO: 18 Files to fix
--
Test-Overview.md
deps.edn
src/madek/api/resources/delegations_users.clj
src/madek/api/resources/edit_sessions.clj
src/madek/api/resources/media_entries.cl
src/madek/api/resources/media_entries/advanced_filter/media_files.clj
src/madek/api/resources/media_entries/advanced_filter/meta_data.clj
src/madek/api/resources/media_entries/advanced_filter/permissions.clj
src/madek/api/resources/media_entries/index.cli
src/madek/api/resources/media_entries/media_entry.cli
src/madek/api/resources/media_files.cl
src/madek/api/resources/media_files/authorization.cl
src/madek/api/resources/media_files/media_file.cli
src/madek/api/resources/media_resources/permissions.clj
src/madek/api/resources/meta_data.cl
src/madek/api/resources/meta_data/index.clj
src/madek/api/resources/meta_data/meta_datum.clj
src/madek/api/resources/meta_keys.clj
src/madek/api/resources/meta_keys/index.clj
src/madek/api/resources/meta_keys/meta_key.clj



Broken tests
---
```bash 
Finished in 6 minutes 35 seconds (files took 1.96 seconds to load)
804 examples, 127 failures

Failed examples:

rspec ./spec/resources/media-entry/edit_permissions_spec.rb:52 # Getting a media-entry resource with authentication check_allowed_if_responsible_user is allowed 200
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:106 # Getting a media-entry resource with authentication check_allowed_if_user_permission is allowed 200
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:133 # Getting a media-entry resource with authentication check_not_allowed_if_updated_user_permission is not allowed 403
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:160 # Getting a media-entry resource with authentication check_not_allowed_if_deleted_user_permission is not allowed 403
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:186 # Getting a media-entry resource with authentication check_allowed_if_group_permission is allowed 200
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:219 # Getting a media-entry resource with authentication check_not_allowed_if_updated_group_permission is not allowed 403
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:251 # Getting a media-entry resource with authentication check_not_allowed_if_deleted_group_permission is not allowed 403
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:273 # Getting a media-entry resource with authentication check_download_allowed_if_user_permission download is allowed 200
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:297 # Getting a media-entry resource with authentication check_edit_permissions_allowed_if_user_permission edit resource perms is allowed 200
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:304 # Getting a media-entry resource with authentication check_edit_permissions_allowed_if_user_permission edit user perms is allowed 200
rspec ./spec/resources/media-entry/edit_permissions_spec.rb:311 # Getting a media-entry resource with authentication check_edit_permissions_allowed_if_user_permission edit group perms is allowed 200
rspec ./spec/resources/media_entries/filter/advanced_filter_spec.rb:18 # advanced filtering of media entries applying a combined filter returns 200 with correct result
rspec ./spec/resources/media_entries/filter/advanced_filter_spec.rb:41 # advanced filtering of media entries searching a string through all meta data returns 200 with correct result
rspec ./spec/resources/media_entries/filter/search_in_not_public_meta_data_spec.rb:17 # search for media entries with meta data from not public vocabulary applying a meta data filter returns 200 with empty result
rspec ./spec/resources/media_entries/index-order-by_spec.rb[1:3:9:1] # ordering media entries created_at madek_core:title ascending order
rspec ./spec/resources/media_entries/index-order-by_spec.rb[1:3:9:2] # ordering media entries created_at madek_core:title descending order
rspec ./spec/resources/media_entries/index-order-by_spec.rb:54 # ordering media entries created_at madek_core:title returns 30 media entries for ascending order
rspec ./spec/resources/media_entries/index-order-by_spec.rb:58 # ordering media entries created_at madek_core:title returns 30 media entries for descending order
rspec ./spec/resources/media_entries/index-order-by_spec.rb:138 # ordering media entries ordering media-entries in a particular set response of ordering by the order attribute of the arc descending arcs nils come last
rspec ./spec/resources/media_entries/index-order-by_spec.rb:222 # ordering media entries ordering media-entries in a particular set a title for each of the entries response of ordering by metadatum string (title usually) has success http state
rspec ./spec/resources/media_entries/index-order-by_spec.rb:226 # ordering media entries ordering media-entries in a particular set a title for each of the entries response of ordering by metadatum string (title usually) media_entries are ordered by metadatum string
rspec ./spec/resources/media_entries/index-order-by_spec.rb[1:4:4:2:1] # ordering media entries ordering media-entries in a particular set ordering by order param madek_core:title ascending order
rspec ./spec/resources/media_entries/index-order-by_spec.rb[1:4:4:2:2] # ordering media entries ordering media-entries in a particular set ordering by order param madek_core:title descending order
rspec ./spec/resources/media_entries/index-order-by_spec.rb:331 # ordering media entries ordering media-entries in a particular set ordering by order param stored_in_collection when collection has title ASC sorting ascending order
rspec ./spec/resources/media_entries/index-order-by_spec.rb:337 # ordering media entries ordering media-entries in a particular set ordering by order param stored_in_collection when collection has title DESC sorting descending order
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:1:1:1:1:1:1] # generated runs ROUND 1 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:1:1:1:1:1:2] # generated runs ROUND 1 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:2:1:1:1:1:1] # generated runs ROUND 2 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:2:1:1:1:1:2] # generated runs ROUND 2 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:3:1:1:1:1:1] # generated runs ROUND 3 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:3:1:1:1:1:2] # generated runs ROUND 3 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:4:1:1:1:1:1] # generated runs ROUND 4 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:4:1:1:1:1:2] # generated runs ROUND 4 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:5:1:1:1:1:1] # generated runs ROUND 5 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:5:1:1:1:1:2] # generated runs ROUND 5 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:6:1:1:1:1:1] # generated runs ROUND 6 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:6:1:1:1:1:2] # generated runs ROUND 6 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:7:1:1:1:1:1] # generated runs ROUND 7 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:7:1:1:1:1:2] # generated runs ROUND 7 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:8:1:1:1:1:1] # generated runs ROUND 8 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:8:1:1:1:1:2] # generated runs ROUND 8 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:9:1:1:1:1:1] # generated runs ROUND 9 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:9:1:1:1:1:2] # generated runs ROUND 9 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:10:1:1:1:1:1] # generated runs ROUND 10 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:10:1:1:1:1:2] # generated runs ROUND 10 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:11:1:1:1:1:1] # generated runs ROUND 11 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:11:1:1:1:1:2] # generated runs ROUND 11 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:12:1:1:1:1:1] # generated runs ROUND 12 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:12:1:1:1:1:2] # generated runs ROUND 12 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:13:1:1:1:1:1] # generated runs ROUND 13 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:13:1:1:1:1:2] # generated runs ROUND 13 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:14:1:1:1:1:1] # generated runs ROUND 14 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:14:1:1:1:1:2] # generated runs ROUND 14 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:15:1:1:1:1:1] # generated runs ROUND 15 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:15:1:1:1:1:2] # generated runs ROUND 15 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:16:1:1:1:1:1] # generated runs ROUND 16 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:16:1:1:1:1:2] # generated runs ROUND 16 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:17:1:1:1:1:1] # generated runs ROUND 17 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:17:1:1:1:1:2] # generated runs ROUND 17 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:18:1:1:1:1:1] # generated runs ROUND 18 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:18:1:1:1:1:2] # generated runs ROUND 18 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:19:1:1:1:1:1] # generated runs ROUND 19 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:19:1:1:1:1:2] # generated runs ROUND 19 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:20:1:1:1:1:1] # generated runs ROUND 20 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:20:1:1:1:1:2] # generated runs ROUND 20 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:21:1:1:1:1:1] # generated runs ROUND 21 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:21:1:1:1:1:2] # generated runs ROUND 21 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:22:1:1:1:1:1] # generated runs ROUND 22 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:22:1:1:1:1:2] # generated runs ROUND 22 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:23:1:1:1:1:1] # generated runs ROUND 23 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:23:1:1:1:1:2] # generated runs ROUND 23 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:24:1:1:1:1:1] # generated runs ROUND 24 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:24:1:1:1:1:2] # generated runs ROUND 24 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:25:1:1:1:1:1] # generated runs ROUND 25 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource status 200
rspec ./spec/resources/meta-data/by-media-resource/meta-datum-json_spec.rb[1:25:1:1:1:1:2] # generated runs ROUND 25 meta_datum_json_for_random_resource_type authenticated_json_client with creator is authed user the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/edit/meta-datum-json_spec.rb:59 # generated runs ROUND 1 edit meta-data-json for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-json_spec.rb:67 # generated runs ROUND 1 edit meta-data-json for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/edit/meta-datum-json_spec.rb:82 # generated runs ROUND 1 edit meta-data-json for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-json_spec.rb:90 # generated runs ROUND 1 edit meta-data-json for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/edit/meta-datum-json_spec.rb:108 # generated runs ROUND 1 edit meta-data-json for random_resource_type authenticated_json_client with creator is authed user update the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-json_spec.rb:116 # generated runs ROUND 1 edit meta-data-json for random_resource_type authenticated_json_client with creator is authed user update the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/edit/meta-datum-json_spec.rb:131 # generated runs ROUND 1 edit meta-data-json for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-json_spec.rb:139 # generated runs ROUND 1 edit meta-data-json for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:61 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:65 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the proper meta-data value
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:70 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the new keywords value
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:85 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:89 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource holds the proper meta-data
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:94 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource holds the new meta-data-keywords value
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:100 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource holds the new keywords ids value
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:106 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource holds the new keywords value
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:128 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource partly status 200
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:132 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource partly holds the proper meta-data
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:137 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource partly holds only undeleted keywords
rspec ./spec/resources/meta-data/edit/meta-datum-keywords_spec.rb:145 # generated runs ROUND 1 edit meta-data-keywords for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource complete deleted keywords
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:62 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:66 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the proper meta-data value
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:71 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the new meta-data-people value
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:77 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the new people_ids value
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:83 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the new people value
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:101 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create and delete the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:105 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create and delete the meta-datum resource holds the proper meta-data value
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:110 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create and delete the meta-datum resource holds the new meta-data-people value
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:114 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create and delete the meta-datum resource holds the new people_ids value
rspec ./spec/resources/meta-data/edit/meta-datum-people_spec.rb:118 # generated runs ROUND 1 edit meta-data-people for random_resource_type authenticated_json_client with creator is authed user create and delete the meta-datum resource holds the new people value
rspec ./spec/resources/meta-data/edit/meta-datum-text-date_spec.rb:52 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-text-date_spec.rb:56 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the proper value
rspec ./spec/resources/meta-data/edit/meta-datum-text-date_spec.rb:71 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-text-date_spec.rb:75 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource holds the proper value
rspec ./spec/resources/meta-data/edit/meta-datum-text-date_spec.rb:93 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user update the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-text-date_spec.rb:97 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user update the meta-datum resource holds the proper value
rspec ./spec/resources/meta-data/edit/meta-datum-text-date_spec.rb:112 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-text-date_spec.rb:116 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource holds the proper value
rspec ./spec/resources/meta-data/edit/meta-datum-text_spec.rb:52 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-text_spec.rb:56 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user create the meta-datum resource holds the proper json value
rspec ./spec/resources/meta-data/edit/meta-datum-text_spec.rb:71 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-text_spec.rb:75 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user read the meta-datum resource holds the proper value
rspec ./spec/resources/meta-data/edit/meta-datum-text_spec.rb:93 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user update the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-text_spec.rb:97 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user update the meta-datum resource holds the proper value
rspec ./spec/resources/meta-data/edit/meta-datum-text_spec.rb:112 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource status 200
rspec ./spec/resources/meta-data/edit/meta-datum-text_spec.rb:116 # generated runs ROUND 1 edit meta-data-text for random_resource_type authenticated_json_client with creator is authed user delete the meta-datum resource holds the proper value
rspec ./spec/resources/meta-data/meta-datum-roles_spec.rb[1:1:1:1:1:1:2:2:1] # generated runs ROUND 1 meta_datum_roles_for_random_resource_type authenticated_json_client with random public view permission the meta-data resource if the response is 200 MetaDatum::Role resource provides valid relations
rspec ./spec/resources/meta-data/meta-datum-roles_spec.rb[1:3:1:1:1:1:2:2:1] # generated runs ROUND 3 meta_datum_roles_for_random_resource_type authenticated_json_client with random public view permission the meta-data resource if the response is 200 MetaDatum::Role resource provides valid relations
rspec ./spec/resources/meta-keys/index_spec.rb:24 # index when user is authenticated when view permission is true returns meta key in collection through the user permissions
rspec ./spec/resources/meta-keys/index_spec.rb:39 # index when user is authenticated when view permission is true returns meta key in collection through the group permissions
rspec ./spec/resources/preview/user_authorization_spec.rb:69 # Getting a preview resource with authentication check_allowed_if_user_permission is allowed 200
rspec ./spec/resources/preview/user_authorization_spec.rb:84 # Getting a preview resource with authentication check_allowed_if_group_permission is allowed 200

```