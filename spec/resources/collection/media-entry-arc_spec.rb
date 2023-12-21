require 'spec_helper'

describe 'Modify the children of a collection' do

    #context 'existing children' do
    include_context :json_client_for_authenticated_user do
        before :each do
            @collection_parent = FactoryBot.create(:collection,
                                                    responsible_user: user)
            @colmearc = FactoryBot.create(:collection_media_entry_arc,
                collection: @collection_parent)
            @me_child = FactoryBot.create(:media_entry)
        end

        let :create_data do
            { 
                order: 1.234567,
                cover: false,
                position: 0,
                highlight: true
            }
        end

        let :create_url do
            "/api/collection/#{@collection_parent.id}/media-entry-arc/#{@me_child.id}"
        end

        let :arc_url do
            "/api/collection/#{@colmearc.collection.id}/media-entry-arc/#{@colmearc.media_entry.id}"
        end


        context 'Responds not authorized without authentication' do
  
  
            describe 'not authorized' do
                it 'query responds with 401' do
                    expect(plain_faraday_json_client.get(arc_url).status).to be == 401
                end

                it 'post responds with 401' do
                    response = plain_faraday_json_client.post(create_url) do |req|
                    req.body = create_data.to_json
                    req.headers['Content-Type'] = 'application/json'
                    end
                    expect(response.status).to be == 401
                end
                it 'put responds with 401' do
                    response = plain_faraday_json_client.put(arc_url) do |req|
                    req.body = {}.to_json
                    req.headers['Content-Type'] = 'application/json'
                    end
                    expect(response.status).to be == 401
                end
            
                it 'delete responds with 401' do
                    expect(plain_faraday_json_client.delete(arc_url).status).to be == 401
                end
            end
        end


        context 'Modify as user' do

            context 'post' do

                it 'before create, query responds with 404' do
                    expect(client.get(create_url).status).to be == 404
                end
            
                let :response do
                    client.post(create_url) do |req|
                        req.body = create_data.to_json
                        req.headers['Content-Type'] = 'application/json'
                    end
                end

                it 'responds with 200' do
                  expect(response.status).to be == 200
                end

                it 'has the proper data' do
                    data = response.body
                    expect(data['media_entry_id']).to be == @me_child.id
                    expect(data['collection_id']).to be == @collection_parent.id
                    expect(
                        data.except("created_at", "updated_at", "id", "media_entry_id", "collection_id")
                    ).to eq(
                        create_data.with_indifferent_access
                        .except(:created_at, :updated_at, :id, :media_entry_id, :collection_id)
                    )
                end
            end


            context 'get' do
                let :response do
                    client.get(arc_url)
                end

                it 'responds with 200' do
                    expect(response.status).to be == 200
                end

                it 'has the proper data' do
                    data = response.body
                    expect(
                        data.except("created_at", "updated_at")
                    ).to eq(
                        @colmearc.attributes.with_indifferent_access
                        .except( :created_at, :updated_at)
                    )
                end
            end


            context 'put' do
                let :response do
                    client.put(arc_url) do |req|
                        req.body = {
                        order: 2.34567,
                        highlight: false
                        }.to_json
                        req.headers['Content-Type'] = 'application/json'
                    end
                end

                it 'responds with 200' do
                    expect(response.status).to be == 200
                end

                it 'has the proper data' do
                    data = response.body
                    expect(
                        data.except("created_at", "updated_at", "order", "highlight")
                    ).to eq(
                        @colmearc.attributes.with_indifferent_access
                        .except( :created_at, :updated_at, :order, :highlight)
                    )
                    expect(data['order']).to be == 2.34567
                    expect(data['highlight']).to be == false
                end
            end

            context 'delete' do            
                let :response do
                    client.delete(arc_url)
                end

                it 'responds with 200' do
                    expect(response.status).to be == 200
                end

                it 'has the proper data' do
                    data = response.body
                    expect(
                        data.except("created_at", "updated_at")
                    ).to eq(
                        @colmearc.attributes.with_indifferent_access
                        .except( :created_at, :updated_at)
                    )
                end
            end

    
    
        end
    
    end
end
