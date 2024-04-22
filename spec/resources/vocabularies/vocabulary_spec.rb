require "spec_helper"

describe "vocabulary" do
  include_context :json_client_for_authenticated_user do
    ###############################################################################
    # Just so that there is some other arbitrary data besides the actual test data.
    # No exlicit expectations are done with them.
    before :example do
      10.times do
        FactoryBot.create(:vocabulary,
          enabled_for_public_view: [true, false].sample)
      end

      Vocabulary.take(5).shuffle.each do |vocabulary|
        Permissions::VocabularyUserPermission.create!(user_id: user.id,
          view: [true, false].sample,
          vocabulary: vocabulary)

        group = FactoryBot.create :group
        group.users << user
        Permissions::VocabularyGroupPermission.create!(group_id: group.id,
          view: [true, false].sample,
          vocabulary: vocabulary)
      end
    end
    ###############################################################################

    def json_vocabulary_resource(vocabulary_id, is_authenticated_user = false)
      if is_authenticated_user
        basic_auth_plain_faraday_json_client(entity.login, entity.password).get("#{api_base_url}/vocabularies/#{vocabulary_id}")
      else
        plain_faraday_json_client.get("#{api_base_url}/vocabularies/#{vocabulary_id}")
      end
    end

    it "should return 200 for an existing vocabulary" do
      vocab = FactoryBot.create(:vocabulary,
        enabled_for_public_view: true)
      expect(
        json_vocabulary_resource(vocab.id).status
      ).to be == 200
    end

    it "should return 404 for non-existing vocabulary" do
      expect(
        json_vocabulary_resource("bla").status
      ).to be == 404
    end

    it "does not return admin_comment property" do
      vocabulary = FactoryBot.create(:vocabulary,
        enabled_for_public_view: true)

      expect(json_vocabulary_resource(vocabulary.id).body)
        .not_to have_key "admin_comment"
    end

    describe "accessibility" do
      it "returns public vocabulary" do
        vocabulary = FactoryBot.create(:vocabulary,
          enabled_for_public_view: true)

        data = json_vocabulary_resource(vocabulary.id).body

        expect(data).to have_key "id"
        expect(data["id"]).to eq vocabulary.id
      end

      it "does not return non-public vocabulary" do
        vocabulary = FactoryBot.create(:vocabulary,
          enabled_for_public_view: false)

        data = json_vocabulary_resource(vocabulary.id).body

        expect(data).not_to have_key "id"
        expect(data["message"]).to eq "Vocabulary could not be found!"
      end

      context "when user is authenticated" do
        context "when view permission is true" do
          it "returns vocabulary through the user permissions" do
            vocabulary = FactoryBot.create(:vocabulary,
              enabled_for_public_view: false)
            Permissions::VocabularyUserPermission.create!(user_id: user.id,
              view: true,
              vocabulary: vocabulary)

            data = json_vocabulary_resource(vocabulary.id, true).body

            expect(data).to have_key "id"
            expect(data["id"]).to eq vocabulary.id
          end

          it "returns vocabulary through the group permissions" do
            vocabulary = FactoryBot.create(:vocabulary,
              enabled_for_public_view: false)
            group = FactoryBot.create :group
            group.users << user
            Permissions::VocabularyGroupPermission.create!(group_id: group.id,
              view: true,
              vocabulary: vocabulary)

            data = json_vocabulary_resource(vocabulary.id, true).body

            expect(data).to have_key "id"
            expect(data["id"]).to eq vocabulary.id
          end
        end

        context "when view permission is false" do
          it "does not return vocabulary through the user permissions" do
            vocabulary = FactoryBot.create(:vocabulary,
              enabled_for_public_view: false)
            Permissions::VocabularyUserPermission.create!(user_id: user.id,
              view: false,
              vocabulary: vocabulary)

            data = json_vocabulary_resource(vocabulary.id, true).body

            expect(data).not_to have_key "id"
            expect(data["message"]).to eq "Vocabulary could not be found!"
          end

          it "does not return vocabulary through the group permissions" do
            vocabulary = FactoryBot.create(:vocabulary,
              enabled_for_public_view: false)
            group = FactoryBot.create :group
            group.users << user
            Permissions::VocabularyGroupPermission.create!(group_id: group.id,
              view: false,
              vocabulary: vocabulary)

            data = json_vocabulary_resource(vocabulary.id, true).body

            expect(data).not_to have_key "id"
            expect(data["message"]).to eq "Vocabulary could not be found!"
          end
        end
      end
    end

    describe "multilingual labels" do
      let(:vocabulary) do
        FactoryBot.create(
          :vocabulary,
          labels: {
            de: "label de",
            en: "label en"
          }
        )
      end

      specify "result contains correct labels" do
        expect(json_vocabulary_resource(vocabulary.id).body["labels"])
          .to eq({"de" => "label de", "en" => "label en"})
      end

      # specify 'result contains a label for default locale' do
      #  expect(
      #    json_vocabulary_resource(vocabulary.id).body['label']
      #  ).to eq 'label de'
      # end
    end

    describe "multilingual descriptions" do
      let(:vocabulary) do
        FactoryBot.create(
          :vocabulary,
          descriptions: {
            de: "description de",
            en: "description en"
          }
        )
      end

      specify "result contains correct descriptions" do
        expect(
          json_vocabulary_resource(vocabulary.id).body["descriptions"]
        )
          .to eq({"de" => "description de", "en" => "description en"})
      end

      # specify 'result contains a description for default locale' do
      #  expect(
      #    json_vocabulary_resource(vocabulary.id).body['description']
      #  ).to eq 'description de'
      # end
    end
  end
end
