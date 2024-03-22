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
      @current_group_users[0..(C / 2.floor)] + @current_non_group_users[0..(C / 2.floor)]

    @update_data = @update_users.map do |user|
      user.slice([:id].sample)
    end

  end

  context 'admin user' do
    include_context :json_client_for_authenticated_admin_user do

      let :response do
        client.put("/api/admin/groups/#{CGI.escape(@group.id)}/users/") do |req|

          puts ">>> /api/admin/groups/#{CGI.escape(@group.id)}/users/"

          req.body = { users: @update_data }.to_json

          puts ">>> #{req.headers}"
          # puts ">>> #{req.body}"

          req.headers['Content-Type'] = 'application/json'
        end

      end

      # TODO: broken
      it 'works and sets the group users to exactly those given with the request', :skip do
        pending "tofix"
        puts ">>> #{response.body}"
        puts ">>> #{response.status}"

        expect(response.status).to be == 200

        expect(
          Set.new(@group.users.reload.map(&:id))
        ).to be == Set.new(@update_users.map(&:id))

      end
    end
  end
end


