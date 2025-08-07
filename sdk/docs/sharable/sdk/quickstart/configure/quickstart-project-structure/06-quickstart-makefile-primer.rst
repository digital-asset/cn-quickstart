Short Makefile primer
=====================

The Quickstart app uses `GNU Make <https://www.gnu.org/software/make/manual/html_node/index.html>`__
as its coreography tool.

The basic format of a `Make <https://en.wikipedia.org/wiki/Make_(software)>`__ build target is:

.. code-block:: text

   .<SPECIAL-TARGET-DECLARATIONS>*: <target-name>
   <target>: <dependency list (space separated)>
         shell commands, make macros, and gnu-make function invocations

From the ``frontend/`` directory, you can run ``npm install && npm run build``.

Or use ``make build-frontend`` from the quickstart/ directory.
In the Makefile, the command's target is:

.. code-block:: text

   .PHONY: build-frontend
   build-frontend: ## Build the frontend application
   @cd frontend && npm install && npm run build

`.PHONY` is a special `built-in target <https://www.gnu.org/software/make/manual/html_node/Phony-Targets.html>`__ used to indicate that build-frontend is strictly a target name and does not correspond to a file.

`build-frontend:` Is a build target that can be invoked directly via `make <target>` or indirectly as a dependency for another target. 
If not marked as a phony-target it is treated as a file, and the last-modified timestamp compared to its dependencies in the usual manner.

`#` is a line comment delimiter, identical to a shell script.

`##` is not a Make concept, but is used by convention as a doc-string to generate the usage displayed by make help.

`<tab>@cd frontend && npm install && npm run build` is a shell command to be executed when the target is invoked. 
Unless this is a phony-target, Make expects this command to regenerate the target file. 
By default, Make prints each shell command to stdout immediately before it executes it; 
this is suppressed if the command is prepended with a `@`.

**NOTE:** *The shell-command* **MUST** *be indented by a literal*
**TAB** *character, the equivalent number of spaces* **WILL NOT
WORK**\ *.*

You can see dependency list in action with the top-level build target:

.. code-block:: text

   .PHONY: build
   build: build-frontend build-backend build-daml build-docker-images

When the target is invoked, the dependency targets are run and brought up to date (or invoked in the case of phony targets) before any shell command is executed.

Other Make features used in the existing file include:

`define` which is used to `define multiline variables <https://www.gnu.org/software/make/manual/html_node/Multi_002dLine.html>`__. In this case,
we use it to define a simple macro (`open-url-target`) to define cross-platform browser interaction targets (try `make open-app-ui` once the application is started for an example). 
The file also includes:

.. code-block:: text

   # Function to run docker-compose with default files and environment
   define docker-compose
   docker compose $(DOCKER_COMPOSE_FILES) $(DOCKER_COMPOSE_ENVFILE) \
   $(DOCKER_COMPOSE_PROFILES) $(1)
   endef

This provides DRY abstraction around calls to `docker-compose`.

`call` which is used to `invoke a variable as a function <https://www.gnu.org/software/make/manual/html_node/Call-Function.html>`__.

Note that the format of a call invocation is: 
`$(call <cmd>[, <args>]*)`. So `$(call open-url-target`, `open-app-ui`, http://localhost:3000) calls `open-url-target` with `$(1)` set to the string `open-app-ui` and `$(2)` set to the URL.

Similarly, the `make status` target uses `$(call docker-compose, ps)` to run `docker-compose ps` with the default arguments. 
This happens via the `docker-compose` function discussed above. 
Removing the `@` allows you to see the expanded command.

.. code-block:: text

   √ % make status
    docker compose -f compose.yaml --env-file .env --profile localnet \
    --env-file docker/localnet.env --profile observability ps

`eval <https://www.gnu.org/software/make/manual/html_node/Eval-Function.html>`__ is used to treat the result of calling `open-url-target` as a macro to define dynamic make targets.
