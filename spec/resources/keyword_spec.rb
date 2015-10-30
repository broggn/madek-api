require 'spec_helper'

context 'Getting a keyword resource without authentication' do
  before :each do
    @keyword = FactoryGirl.create :keyword
  end

  let :plain_json_response do
    plain_faraday_json_client.get("/api/keywords/#{@keyword.id}")
  end

  let :json_roa_keyword_resource do
    JSON_ROA::Client.connect("#{api_base_url}/keywords/#{@keyword.id}")
  end

  it 'responds with 200' do
    expect(json_roa_keyword_resource.get.response.status)
      .to be == 200
    expect(
      json_roa_keyword_resource
        .get.relation('meta-key').get.response.status
    ).to be == 200
    expect(
      json_roa_keyword_resource
        .get.relation('root').get.response.status
    ).to be == 200
    expect(plain_json_response.status).to be == 200
  end
end