require 'spec_helper'

context 'groups' do

  before :each do
    @groups = 201.times.map{FactoryBot.create :group}
  end

  context 'non admin user' do
    include_context :json_client_for_authenticated_user do
      it 'is forbidden to retrieve groups' do
        expect(
          client.get('/api/admin/groups/').status
        ).to be==403
      end
    end
  end

  context 'admin user' do
    include_context :json_client_for_authenticated_admin_user do

      describe 'get groups' do

        let :groups_result do
          client.get("/api/admin/groups/?count=100")
        end

        it 'responses with 200' do
          expect(groups_result.status).to be== 200
        end

        it 'returns some data but less than created because we paginate' do
          expect(
            groups_result.body['groups'].count
          ).to be< @groups.count
        end

        # TODO json roa remove: get groups collection by id
        #it 'using the roa collection we can retrieve all groups' do
        #  set_of_created_ids = Set.new(@groups.map(&:id))
        #  set_of_retrieved_ids = Set.new(groups_result.collection().map(&:get).map{|x| x.data['id']})
        #  expect(set_of_retrieved_ids.count).to be== set_of_created_ids.count
        #  expect(set_of_retrieved_ids).to be== set_of_created_ids
        #end

      end
    end
  end
end


