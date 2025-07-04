Short Makefile primer
=====================

Make is the original build tool developed to assist with C development
on UNIX in 1976 [36]_. As such it relies heavily on transparent
integration with the Unix shell. To this day, Make retains the most
comprehensive and seamless shell integration of any build tool available
— which is why it makes a good choreography tool. The version used in
this project is GNU Make [37]_, which has a number of useful extensions.

The basic format of a Make build target is:

.. code-block:: text

   .<SPECIAL-TARGET-DECLARATIONS>*: <target-name>
   <target>: <dependency list (space separated)>
         shell commands, make macros, and gnu-make function invocations

For instance to build the frontend you can run `npm install && npm run build`
from the `frontend/` directory, or make build-frontend from the
quickstart/ directory via the following target in quickstart/Makefile:

.. code-block:: text

   .PHONY: build-frontend
   build-frontend: ## Build the frontend application
   @cd frontend && npm install && npm run build

`.PHONY` is a special built-in target that is used to indicate that
build-frontend is strictly a target name and does not correspond to a
file [38]_.

`build-frontend:` Is a build target that can be invoked directly via
`make <target>` or indirectly as a dependency for another target. If not marked
as a phony-target it is treated as a file, and the last-modified
timestamp compared to its dependencies in the usual manner.

`#` is a line comment delimiter, identical to a shell script.

`##` is not a Make concept, but is used by convention as a doc-string to
generate the usage displayed by make help.

`<tab>@cd frontend && npm install && npm run build` is a shell command to
be executed when the target is invoked. Unless this is a phony-target,
Make's expectation is that this command will regenerate the target file. By
default, Make prints each shell command to stdout immediately before it
executes it; this is suppressed if the command is prepended with a `@`.

**NOTE:** *The shell-command* **MUST** *be indented by a literal*
**TAB** *character, the equivalent number of spaces* **WILL NOT
WORK**\ *.*

You can see dependency list in action with the top-level build target:

.. code-block:: text

   .PHONY: build
   build: build-frontend build-backend build-daml build-docker-images

When the target is invoked the dependency targets are run to bring them
up to date (or invoked in the case of phony targets) before any shell
command is executed.

Other Make features that are currently used in the existing file
include:

`define` [39]_ which is used to define multiline variables. In this case,
we use it to define a simple macro (`open-url-target`) to define
cross-platform browser interaction targets (try `make open-app-ui` once
the application is started for an example). The file also includes:

.. code-block:: text

   # Function to run docker-compose with default files and environment
   define docker-compose
   docker compose $(DOCKER_COMPOSE_FILES) $(DOCKER_COMPOSE_ENVFILE) \
   $(DOCKER_COMPOSE_PROFILES) $(1)
   endef

This provides DRY abstraction around calls to `docker-compose`.

`call` [40]_ which is used to invoke a variable as a function.

Note that the format of a call invocation is: `$(call <cmd>[, <args>]*)`. So
`$(call open-url-target`, `open-app-ui`, http://localhost:3000) calls
`open-url-target` with `$(1)` set to the string `open-app-ui` and `$(2)` set to
the URL.

Similarly, the `make status` target uses `$(call docker-compose, ps)` to run
`docker-compose ps` with the default arguments. This happens via the
`docker-compose` function discussed above. Removing the `@` allows you
to see the expanded command.

.. code-block:: text

   √ % make status
    docker compose -f compose.yaml --env-file .env --profile localnet \
    --env-file docker/localnet.env --profile observability ps

`eval` [41]_ which is used to treat the result of calling `open-url-target`
as a macro to define dynamic make targets.

.. [36]
   https://en.wikipedia.org/wiki/Make_(software)

.. [37]
   https://www.gnu.org/software/make/manual/html_node/index.html

.. [38]
   https://www.gnu.org/software/make/manual/html_node/Phony-Targets.html

.. [39]
   https://www.gnu.org/software/make/manual/html_node/Multi_002dLine.html

.. [40]
   https://www.gnu.org/software/make/manual/html_node/Call-Function.html

.. [41]
   https://www.gnu.org/software/make/manual/html_node/Eval-Function.html
