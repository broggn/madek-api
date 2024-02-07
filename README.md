# The Madek API2

The madek-api2 is a JSON API for Madek.


## API v2 TODOs

* make DB Audits works
* upgrade to clj-jdbc-next and honeysql2
* some resources, like `admins` are likely to change in the not to fare future:
    hide most of that behind generic resources or properties



## Development

Requirements:

* PostgreSQL 15 Database
* `asdf` https://asdf-vm.com/
* system build tools and libaries; e.g. `sudo apt-get install build-essential` on ubuntu;
    on MacOS you will need Xcode with command line tools and further packages either from
    MacPorts or Homebrew
*  ⚠️ WARNING: local tests can fail because of wrong order of results (see terms_for_sorting_shared_context.rb)


### Starting up the Server

    ./bin/clj-run

### Running the Tests

Rspec should be invoked from `./bin/rspec`

        ./bin/rspec ./spec/resources/groups/index_spec.rb:11




### Formatting Code

Use `./bin/cljfmt check` and  `./bin/cljfmt fix`.

From vim you can use `:! ./bin/cljfmt fix %` to format the current file.



### API Docs

Swagger resource documentation http://localhost:3104/api-docs/index.html

### Sever Configuration

NOTE: whilst switching to jdbc-next the database must be configuration both in
the config file `config/settings.local.yml` and via environment variables (or cli
arguments).


Set PG environment variables like PGPORT, PGDATABASE, PGUSER, etc.

Create a config/settings.local.yml with content similar like:

    database:
      url: postgresql://localhost:5415/madek?pool=3


### Test Configuration

The tests need a rails like configuration:

    cp datalayer/config/database_dev.yml spec/config/database.yml

should be sufficient.

