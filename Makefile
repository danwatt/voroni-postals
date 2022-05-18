define run_in_docker
	docker compose run -e MAVEN_TARGET=$(1) builder
endef

# Build the builder image
builder-image:
	docker compose build builder

# verify using Docker, to emulate what is being done on the Go agent
verify: builder-image
	$(call run_in_docker,"clean verify")

run: builder-image
	$(call run_in_docker,"clean package")
	docker compose up --build web