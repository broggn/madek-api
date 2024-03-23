require 'spec_helper'

context 'users' do

  before :each do
    @users = 201.times.map{FactoryBot.create :user}
  end



  context 'admin user' do
    include_context :json_client_for_authenticated_admin_user do

      # more tests needed for query parameters:
      #   * email,
      #   * login,
      #   * institution,
      #   * institutional_id,
      #   *  and full text search

      describe 'get users' do

        let :users_result do
          client.get("/api/admin/users/?count=100")
        end

        it 'responses with 200' do
          # TODO
          # binding.pry

          # method=:get,
          # body={"msg"=>"null"},
          # url=#<URI::HTTP http://localhost:3104/api/admin/users/?count=100>,



          expect(users_result.status).to be== 200
        end

        it 'returns some data but less than created because we paginate' do

          # TODO
          # binding.pry
          #
          # [1] pry(#<RSpec::ExampleGroups::Users::AdminUser::GetUsers>)> users_result.body
          # >> url=http://localhost:3104/api
          # => {"msg"=>"null"}


          expect(
            users_result.body['users'].count
          ).to be< @users.count
        end

      end
    end
  end
end
