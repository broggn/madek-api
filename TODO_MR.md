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

Needed changes
--
1. GET parameters
   1. Fetching attributes by `(-> request :path-params)` instead of `(-> request :parameters :path)`
   2. Use `path` instead of `query`   
      ```clojure
                 :swagger {:produces "application/json"
                     :parameters [{:name "id"
                                   :in "path"    
                                   ;:in "query"    ;; broken
      ```

ToAsk
--
1. How are `auth-enttity/is_admin` use?
   1. https://github.com/Madek/madek-api-v2/blob/master/src/madek/api/authentication.clj#L10-L26
   2. https://github.com/Madek/madek-api-v2/blob/master/src/madek/api/utils/auth.clj#L10-L35
2. ~~How to define **default-values/description for attribute?~~
   -  Swagger-Editor: https://editor.swagger.io/, see example [_tmp_doc/swagger-example.yml](_tmp_doc/swagger-example.yml)
      - Response header
      - Default-value & description
3. Individual basic-auth for each endpoint or for whole site?
4. Introduction of different LevelOfDetails (LoD) concerning GET-response
   1. All attributes (incl. created/update) (default for admin endpoints )
   2. Min. attributes (default)
5. Introduction of pagination for GET-Requests
   1. page / page-size
   2. headers.x-count
6. Set defaults for all endpoints
7. Links? `application/json vs application/json-roa`


FYI
--
1. Added a headers-parsing function for headers
   1. headers.id & headers.is_admin to be 
2. swagger-ui bug _> https://github.com/swagger-api/swagger-ui/issues/8007
3. [Multiple response-examples per status](https://stackoverflow.com/questions/36576447/swagger-specify-two-responses-with-same-code-based-on-optional-parameter) 
   > OpenAPI 2.0
   > OAS2 does not support multiple response schemas per status code. You can only have a single schema, for example, a free-form object (type: object without properties).



Swagger-UI Validation
--
```clojure
  :swagger {:produces "application/json"
            :parameters [{:name "id"
                          :in "path"
                          :description "e.g.: madek_core:subtitle"
                          :type "string"
                          :required true
                          :pattern "^16.*$"
                          }]
            }
```

```clojure
  :swagger {:produces "application/json"
            :parameters [{:name "id"
                          :in "path"
                          :description "e.g.: madek_core:subtitle"
                          :type "string"
                          :required true
                          :pattern "^16.*$"

                          :nullable false ; has no effect
                          :allowEmptyValue false  ; has no effect
                          }]
            }
```

## Customized validation-message
> 
```clojure
     :swagger {:produces "application/json"
               :parameters [{:name "id"
                             :in "path"
                             :description "e.g.: madek_core:subtitle"
                             :type "string"
                             :required true
                             :pattern "^[a-z0-9\\-\\_\\:]+:[a-z0-9\\-\\_\\:]+$"

    ; working example
    :name "id"
    :pattern "^[a-z0-9\\-\\_\\:]+:[a-z0-9\\-\\_\\:]+$"                          

    
    ; Has no effect
    :x-schema {:pattern #"[a-z0-9\\-\\_\\:]+:[a-z0-9\\-\\_\\:]+"}
    :x-error-message "Invalid ID format. The ID must match the pattern 'some_pattern'."
    :error-message "jfkdsl"
```



## Concrete response #1
```clojure

:responses {200 {:body schema_export-meta-key-usr}

            404 {:description "No entry found for the given id"
                 :schema s/Str
                 :examples {"application/json" {:message "No such entity in :meta_keys as :id with not-existing:key"}}}

            422 {:body {:message s/Str}}}
```

## Concrete response #2
```clojure

:responses {
            200 {
                 :description "Meta-Keys-Object that contians list of meta-key-entries OR empty list"
                 :body {:meta-keys [schema_export-meta-key-usr]}
                 }
            }
```


## Concrete parameters example
```clojure
                {:name "count"
                 :in "query"
                 :description "Number of items per page, defaults to 100"
                 :required true
                 :value 100
                 :default 100
                 :type "number"
                 :pattern "^[1-9][0-9]*$"
                 }
```



            :description "Get list of meta-key ids. Paging is used as you get a limit of 100 entries.


  <div style=\"color: green\">
            - Get list of meta-key ids. Paging is used as you get a limit of 100 entries.
            - Get list of meta-key ids. Paging is used as you get a limit of 100 entries

            ### Get list of meta-key ids. Paging is used as you get a limit of 100 entries.

                      ```clojure
                      (defn example []
                      (println \"Hello, world!\"))
                     ```

                     </div>

            "