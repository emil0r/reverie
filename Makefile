#!/usr/bin/env make -f

default:
	@echo "Check commands"


# linting

lint-core:
	@echo "Lint core"
	clj-kondo --lint reverie-core/src

lint-sql:
	@echo "Lint sql"
	clj-kondo --lint reverie-sql/src

lint:	lint-core \
	lint-sql


lein-install-core:
	@echo "leiningen install core"
	(cd reverie-core && lein install)

lein-install-sql:
	@echo "leiningen install sql"
	(cd reverie-sql && lein install)

lein-install:	lein-install-core \
		lein-install-sql
