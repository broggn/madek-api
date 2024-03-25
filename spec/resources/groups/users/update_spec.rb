require 'spec_helper'

describe 'updating group-users' do

  before :each do

    C = 110

    @group = FactoryBot.create :institutional_group

    @current_group_users = C.times.map do
      FactoryBot.create :user, institutional_id: SecureRandom.uuid
    end

    @group.users << @current_group_users


    @current_non_group_users = C.times.map do
      FactoryBot.create :user, institutional_id: SecureRandom.uuid
    end

    @update_users = \
      @current_group_users[0..(C/2.floor)] + @current_non_group_users[0..(C/2.floor)]

    @update_data= @update_users.map do |user|
      user.slice([:id].sample)
    end

  end

  context 'admin user' do
    include_context :json_client_for_authenticated_admin_user do

      let :response do
        client.put("/api/admin/groups/#{CGI.escape(@group.id)}/users/") do |req|
          req.body = {users: @update_data}.to_json
          req.headers['Content-Type'] = 'application/json'
        end
      end

      it 'works and sets the group users to exactly those given with the request' do
        binding.pry

        # method=:put,
        #   body={"msg"=>"ERROR: duplicate key value violates unique constraint \"index_groups_users_on_user_id_and_group_id\"\n  Detail: Key (user_id, group_id)=(942035a7-2f1c-438a-9d58-7f90c3d4f01d, 07d832a8-d349-458e-9256-1109ccc3972a) already exists."},
        #   url=#<URI::HTTP http://localhost:3104/api/admin/groups/07d832a8-d349-458e-9256-1109ccc3972a/users/>,
        #     request=#<struct Faraday::RequestOptions params_encoder=nil, proxy=nil, bind=nil, timeout=nil, open_timeout=nil, write_timeout=nil, boundary=nil, oauth=nil, context=nil>,
        #       request_headers={"Accept"=>"application/json", "User-Agent"=>"Faraday v0.17.6", "Content-Type"=>"application/json", "Authorization"=>"Basic ZGFycmVsbGIyM2RkZDYwOlRPUFNFQ1JFVA=="},


          expect(response.status).to be== 200
        expect(
          Set.new(@group.users.reload.map(&:id))
        ).to be== Set.new(@update_users.map(&:id))

      end
    end
  end
end


