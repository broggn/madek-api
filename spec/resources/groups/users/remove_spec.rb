require "spec_helper"

context "removing a user from a group via DELETE" do
  before :each do
    @group = FactoryBot.create :institutional_group
    @user = FactoryBot.create :user, institutional_id: SecureRandom.uuid
    @group.users << @user
  end

  it "the user does belong to the group" do
    expect(@group.users.reload.map(&:id)).to include(@user[:id])
  end

  context "admin user" do
    include_context :json_client_for_authenticated_admin_user do
      describe "removing a user from the group via DELETE" do
        it "responds with 204" do
          expect(
            client.delete("/api/admin/groups/#{CGI.escape(@group.id)}/users/#{CGI.escape(@user.id)}").status
          ).to be == 200
        end

        it "effectively removes the user from the group" do
          client.delete("/api/admin/groups/#{CGI.escape(@group.id)}/users/#{CGI.escape(@user.id)}")
          expect(@group.users.reload.map(&:id)).not_to include(@user[:id])
        end
      end
    end
  end
end
