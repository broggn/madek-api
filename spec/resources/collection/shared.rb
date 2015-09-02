require 'spec_helper'

shared_context :collection_resource_via_json_roa do
  let :resource do
    json_roa_client.get.relation('collection') \
      .get('id' => @collection.id)
  end
  let :response do
    resource.response
  end
end

shared_context :auth_collection_resource_via_json_roa do
  let :client do
    json_roa_client do |conn|
      conn.basic_auth(@entity.login, @entity.password)
    end
  end
  let :resource do
    client.get.relation('collection').get('id' => @collection.id)
  end
  let :response do
    resource.response
  end
end

shared_context :collection_resource_via_plain_json do
  let :response do
    plain_faraday_json_client.get("/api/collections/#{@collection.id}")
  end
end

shared_context :check_collection_resource_via_any do |ctx|
  context :via_json_roa do
    include_context :collection_resource_via_json_roa
    include_context ctx
  end

  context :via_plain_json do
    include_context :collection_resource_via_plain_json
    include_context ctx
  end
end
