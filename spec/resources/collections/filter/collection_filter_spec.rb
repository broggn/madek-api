require 'spec_helper'

describe 'filtering collections' do
  
  def get_collections(filter = nil)

    puts ">>0> #{filter}"

    # ab = data client.get('/api/collections/', filter)
    # binding.pry

    ccc = client.get('/api/collections', filter)
    # puts ">>1> #{c


    puts ">>1a #{ccc.headers}"
    puts ">>1b #{ccc.body}"

    aaa = ccc.body

    # binding.pry

    puts ">>2> #{aaa}"
    # binding.qqqqqqqpry

    aaa.with_indifferent_access['collections']
  end

  context 'by collection_id' do
    include_context :json_client_for_authenticated_user do
      it 'as single filter option' do
        @collection = FactoryBot.create(:collection)
        5.times do
          @collection.collections << FactoryBot.create(:collection)
        end
        get_collections('collection_id' => @collection.id)
          .each do |c|
          collection = Collection.unscoped.find(c['id'])
          expect(@collection.collections).to include collection
        end
      end

      it 'combined with other filter option' do
        @collection = FactoryBot.create(:collection)
        collection_1 = FactoryBot.create(:collection,
                                           get_metadata_and_previews: false)
        collection_2 = FactoryBot.create(:collection,
                                           get_metadata_and_previews: true)
        collection_3 = FactoryBot.create(:collection,
                                           get_metadata_and_previews: false)
        collection_3.user_permissions << \
          FactoryBot.create(:collection_user_permission,
                             user: user,
                             get_metadata_and_previews: true)
        [collection_1, collection_2, collection_3].each do |c|
          @collection.collections << c
        end

        response = get_collections('collection_id' => @collection.id,
                                   'me_get_metadata_and_previews' => true)


        # [11] pry(#<RSpec::ExampleGroups::FilteringCollections::ByCollectionId>)> get_collections('collection_id' => @collection.id,
        # >>0> {"collection_id"=>"ed08c313-0580-4567-a3b4-6de61b866ab3", "me_get_metadata_and_previews"=>true}
        # >>1a {"content-type"=>"application/json; charset=utf-8", "access-control-allow-credentials"=>"true", "content-length"=>"67", "server"=>"http-kit", "date"=>"Sat, 23 Mar 2024 22:12:46 GMT"}
        # >>2> {"msg"=>"These SQL clauses are unknown or have nil values: :offset"}
        #
        #
        # binding.pry
        puts ">>1> #{response}"

        # expect(response.count).to be == 2  ;; TODO: fixme
        response.each do |me|
          collection = Collection.unscoped.find(me['id'])
          expect(collection).not_to be == collection_1
          expect(@collection.collections).to include collection
        end
      end
    end
  end
end
