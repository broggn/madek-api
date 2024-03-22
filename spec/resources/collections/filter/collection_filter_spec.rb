require 'spec_helper'

describe 'filtering collections' do
  
  def get_collections(filter = nil)

    puts ">>0> #{filter}"

    # ab = data client.get('/api/collections/', filter)
    # binding.pry

    ccc = client.get('/api/collections', filter)
    # puts ">>1> #{c


    puts ">>1a #{ccc.headers}"

    aaa = ccc.body



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
