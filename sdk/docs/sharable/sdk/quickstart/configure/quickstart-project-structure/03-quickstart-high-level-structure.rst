Canton Quickstart high level structure
=======================================

The Canton Quickstart project extends the ``LocalNet`` Docker Compose configuration from the 
`Splice repository <https://docs.sync.global/app_dev/testing/localnet.html#>`__ using a modular architecture. 
It organizes additional capabilities, including, authentication, observability, PQS, 
and application configurations as discrete Docker Compose modules that layer onto ``LocalNet``'s base infrastructure.

The ``LocalNet`` configuration runs local versions of a super validator, 
the Canton Coin wallet application, the example application nodes, and any supporting services. 
This allows you to run, test, and demonstrate the application entirely on a single machine.

This is sufficient for local development, debugging, and testing.
For production, consider dynamic resource management and high-availability support and other infrastructure requirements beyond the local development foundation.

Configuration
-------------

Running ``make setup`` in the ``quickstart/`` directory selects a configuration.

Once your configuration is built and running, ``make status`` in ``quickstart/`` displays the associated running Docker containers.

See the Topology documentation in the ``docs/`` directory for more detailed information on the various nodes in each configuration and their relationship to each other.

Top-level
---------

The top-level project directory supports a portable, consistent cross-platform development environment. 
It does this using the `Nix <https://nixos.org/download/>`__ package manager, `Direnv <https://direnv.net/>`__, and `Docker Compose <https://docs.docker.com/compose/>`__.
If you do not wish to use Nix, ``quickstart/`` directory can be made the top-level directory for your project, however, you will need to manage your binary dependencies manually. 
Review the `Canton Utility Setup <https://docs.digitalasset.com/utilities/0.7/canton-utility-setup/utility-setup.html>`__ if you require utility deployment support. 

The current top-level directory contents include:

.. code-block:: text

   % ls -Algo
   total 120
   drwxr-xr-x   6     192 Aug 18 13:56 .circleci
   drwxr-xr-x   2      64 Aug  5 08:23 .direnv
   -rw-r--r--   1     388 Aug 18 13:56 .envrc
   drwxr-xr-x  15     480 Aug 20 12:32 .git
   -rw-r--r--   1     214 Feb 14  2025 .gitattributes
   drwxr-xr-x   3      96 Aug 18 13:56 .github
   -rw-r--r--   1     652 Aug 18 13:56 .gitignore
   drwxr-xr-x   3      96 Apr 14 13:46 .vscode
   drwxr-xr-x   3      96 Aug 18 17:44 docs
   -rw-r--r--   1    1462 Aug 18 13:56 flake.lock
   -rw-r--r--   1     547 Aug 18 13:56 flake.nix
   -rw-r--r--   1     680 Feb 14  2025 LICENSE
   drwxr-xr-x   5     160 Aug 18 13:56 nix
   drwxr-xr-x  27     864 Aug 18 13:56 quickstart
   -rw-r--r--   1   24258 Aug 18 13:56 README.md
   drwxr-xr-x  12     384 Aug 19 11:39 sdk
   -rw-r--r--   1     702 Feb 14  2025 SECURITY.md
   -rw-r--r--   1    7042 Aug 18 13:56 terms.md

``.git*`` The usual git files and directories. 
``.gitignore`` is configured to exclude build artifacts for the build systems in use, Daml SDK support files, and IDE project artifacts.

``.envrc`` is a part of the Direnv configuration. 
Specifically, it activates the Nix environment for the project via a call to ``use nix`` which uses the ``shell.nix`` file to set up the development environment using `nix-shell <https://nixos.wiki/wiki/Development_environment_with_nix-shell>`__.

``docs`` contains some engineering documentation for the example app.

``quickstart`` is the main project directory. 

``nix`` contains the Nix configuration. 
`shell.nix <https://nix.dev/tutorials/first-steps/declarative-shell.html>`__ manages new dependencies.

``nix/sources.json`` pins the nix release for determinacy across builds. 
Update at a cadence that balances staying up to date with development environment stability.

``sdk/`` contains the source to this documentation, using reStructuredText.

**Current Dependencies declared in `nix/shell.nix`**
   - jdk21
   - nodejs_20
   - typescript
These are in addition to the `Nix stdenv environment <https://nixos.org/manual/nixpkgs/stable/#sec-tools-of-stdenv>`__.
