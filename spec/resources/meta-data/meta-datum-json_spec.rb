require 'spec_helper'
require 'json'
require Pathname(File.expand_path('..', __FILE__)).join('shared')

describe 'generated runs' do
  (1..ROUNDS).each do |round|
    # (1..1).each do |round|
    describe "ROUND #{round}" do
      describe 'meta_datum_json_for_random_resource_type' do
        include_context :meta_datum_for_random_resource_type
        let(:meta_datum_json) { meta_datum "json" }

        describe 'authenticated_json_client' do
          include_context :authenticated_json_client
          after :each do |example|
            if example.exception
              example.exception.message << \
                "\n  MediaResource: #{media_resource} " \
                  " #{media_resource.attributes}"
              example.exception.message << "\n  Client: #{client_entity} " \
                " #{client_entity.attributes}"
            end
          end

          describe 'with random public view permission' do
            before :each do
              media_resource.update! \
                get_metadata_and_previews: (rand <= 0.5)
            end

            describe 'the meta-datum resource' do
              let :response do
                authenticated_json_client.get("/api/meta-data/#{meta_datum_json.id}")
              end

              let :value do
                response.body['value']
              end

              let :json_value do
                JSON.parse value
              end

              it 'status, either 200 success or 403 forbidden, ' \
                   'corresponds to the get_metadata_and_previews value' do
                expect(response.status).to be == \
                                             (media_resource.get_metadata_and_previews ? 200 : 403)
              end

              it 'holds the proper json value when the response is 200' do
                if response.status == 200
                  expect(json_value).to be == meta_datum_json.json
                end
              end
            end

            describe 'the meta-datum-data-stream resource' do
              let :response do
                authenticated_json_client.get("/api/meta-data/#{meta_datum_json.id}/data-stream")
              end

              it 'status, either 200 success or 403 forbidden, ' \
                   'corresponds to the get_metadata_and_previews value' do
                expect(response.status).to be == \
                                             (media_resource.get_metadata_and_previews ? 200 : 403)
              end

              it 'holds the proper json value when the response is 200' do
                if response.status == 200
                  expect(response.body).to be == meta_datum_json.json
                end
              end

              it 'sets the proper header when the response is 200' do
                if response.status == 200
                  expect(/^application\/json;|^application\/json$/).to \
                    match response.headers['content-type']
                end
              end
            end
          end
        end
      end
    end
  end
end
