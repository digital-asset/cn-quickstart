# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

############################################################################
####  Docker Compose Functions
############################################################################

## Function takes an argument and list of values and maps each list item to
#  the provided argument.
#  Usage:
#    $(1) == argument flag to prepend per item
#    $(2) == list of items
define return_as_args
  $(addprefix $(1) ,$(2))
endef

## Function constructs docker compose commands using the values from the 
#  following variables:
#    - DOCKER_COMPOSE_FILES
#    - DOCKER_COMPOSE_ENVFILES
#    - DOCKER_COMPOSE_PROFILES 
#  Usage:
#    $(1) == arguments to be passed to constructed docker compose command
define docker-compose
	docker compose \
	  $(call return_as_args,-f,$(DOCKER_COMPOSE_FILES)) \
	  $(call return_as_args,--env-file,$(DOCKER_COMPOSE_ENVFILES)) \
		$(call return_as_args,--profile,$(DOCKER_COMPOSE_PROFILES)) \
		$(1)
endef