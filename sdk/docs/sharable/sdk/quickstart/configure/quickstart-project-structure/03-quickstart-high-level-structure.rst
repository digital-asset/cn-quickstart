Canton Quickstart high level structure
=======================================

The Canton Quickstart project builds upon the `LocalNet` docker compose 
local test configuration provided within the
`Splice repository <https://docs.sync.global/app_dev/testing/localnet.html#>`.
Specifically Quickstart uses a module convention to extend the default
Validator and Super-Validator provided by Splice with authentication,
observability, PQS, and application configurations.

For simplicity Quickstart restricts itself to what is possible with
`docker-compose`. This is sufficient for local development, debugging, and
testing; however, for production you should consider if the dynamic resource
management, high-availability support provided by Kubernetes is required.

The `LocalNet` configuration runs local versions of a super validator, the
Canton Coin wallet application, the example application nodes, and any
supporting services.  This allows you to run, test, and demonstrate the
application entirely on a single machine. Please note that depending on your
configuration, this can require considerable memory resources [5]_.

Running `make setup` in the `quickstart/` directory allows you to select
a configuration.

Once your configuration is built and running, `make status` in `quickstart/`
displays the associated running Docker containers.

See the Topology documentation in the `docs/` directory for more detailed
information on the various nodes in each configuration and their
relationship to each other.

Top level
---------

Most of the top-level project directory is associated with supporting a
portable, consistent cross-platform development environment. It does
this using the package manager
`Nix <https://nixos.org/download/>`__\  [6]_,
`Direnv <https://direnv.net/>`__\  [7]_, and `Docker
Compose <https://docs.docker.com/compose/>`__\  [8]_. The top-level setup
ensures a consistent and repeatable dev, build, and test regardless of
choice of environment.

The current toplevel directory contents for a fresh checkout include:

.. code-block:: text

   √ % ls -Algo
   total 60
   -rw-r--r--  1   427 Jul  2 16:38 .envrc
   drwxr-xr-x 15   480 Jul  7 19:15 .git
   -rw-r--r--  1   214 Jul  2 16:38 .gitattributes
   drwxr-xr-x  3    96 Jul  2 16:38 .github
   -rw-r--r--  1   633 Jul  2 16:38 .gitignore
   -rw-r--r--  1   680 Jul  2 16:38 LICENSE
   -rw-r--r--  1 26180 Jul  7 19:13 README.md
   -rw-r--r--  1   702 Jul  2 16:38 SECURITY.md
   drwxr-xr-x  3    96 Jul  2 16:38 docs
   drwxr-xr-x  4   128 Jul  2 16:38 nix
   drwxr-xr-x 26   832 Jul  7 19:13 quickstart
   drwxr-xr-x  3    96 Jul  2 16:38 sdk
   -rw-r--r--  1   881 Jul  2 16:38 shell.nix
   -rw-r--r--  1  7042 Jul  2 16:38 terms.md

`.git*` The usual git files and directories. In particular, `.gitignore` is
configured to exclude build artifacts for the current build systems in
use; Daml SDK support files; and, IDE project artifacts for Visual Code
or other IDEs.

`.envrc` This is a part of the Direnv configuration. Specifically it
activates the Nix environment for the project via a call to `use flake`
which uses the `flake.nix` file to set up the development environment
using nix-shell [9]_.

`LICENSE`, `terms.md`, `Security.md`, and `README.md`. The License is 0BSD.

`docs/` contains some engineering documentation for the example app.

`quickstart/` is the main project directory. If you do not wish to use
Nix, this directory can be made the toplevel directory for your project
— although you will then need to manage your binary dependencies
manually. The next section covers this directory in detail.

`shell.nix` [10]_ and `nix/` contain the Nix configuration. Familiarity with
shell.nix is essential, as it manages new dependencies. Note
`nix/sources.json` pins the nix release to ensure determinacy across
builds. You will want to ensure this gets updated at an appropriate
cadence that balances staying up to date with development environment
stability.

`sdk/` contains the source to this documentation, using reStructuredText.

**Current Dependencies declared in `nix/shell.nix`**
   - jdk21
   - nodejs_20
   - typescript

These are in addition to the Nix stdenv environment [11]_.

.. [5]
   While writing this guide, the author’s Docker configuration was 10 CPUs & 25GB RAM

.. [6]
   https://nixos.org/download/

.. [7]
   https://direnv.net/

.. [8]
   https://docs.docker.com/compose/

.. [9]
   https://nixos.wiki/wiki/Development_environment_with_nix-shell

.. [10]
   https://nix.dev/tutorials/first-steps/declarative-shell.html

.. [11]
   https://nixos.org/manual/nixpkgs/stable/#sec-tools-of-stdenv
