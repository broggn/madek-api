require "spec_helper"

shared_context :auth_media_file_resource_via_json do
  let :client do
    basic_auth_plain_faraday_json_client(@entity.login, @entity.password)
  end
  let :url do
    "/api/media-file/#{@media_file.id}"
  end
  let :response do
    client.get(url)
  end
end

shared_context :auth_media_file_original_data_stream_via_json do
  let :client do
    basic_auth_plain_faraday_json_client(@entity.login, @entity.password)
  end
  let :url do
    "/api/media-file/#{@media_file.id}/data-stream"
  end
  let :response do
    client.get(url)
  end
end

shared_context :media_file_resource_via_plain_json do
  let :response do
    plain_faraday_json_client.get("/api/media-file/#{@media_file.id}")
  end
end

shared_context :check_media_file_resource_via_any do |ctx|
  # TODO via session
  # TODO via token

  context :via_plain_json do
    include_context :media_file_resource_via_plain_json
    include_context ctx
  end
end
