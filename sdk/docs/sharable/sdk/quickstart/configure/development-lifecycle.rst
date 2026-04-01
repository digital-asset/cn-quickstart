.. _quickstart-development-lifecycle:

Development lifecycle
=====================

**Contents**

`Development lifecycle <#development-lifecycle>`__

   `Learning phase <#learning-phase>`__

   `Experimentation phase <#experimentation-phase>`__

   `Development phase <#development-phase>`__

   `Gradle settings <#gradle-settings>`__

   `Environment variables <#environment-variables>`__

   `Docker Compose <#docker-compose>`__

   `Separation phase <#separation-phase>`__

   `Ongoing updates <#ongoing-updates>`__

We’ve observed five distinct phases of the CN Quickstart development journey.
Each phase presents unique strategies for interacting with the CN Quickstart.

Learning phase
---------------

(½ - 2 weeks)

Often the first interaction with the CN Quickstart is understanding how to get the environment running. 
The next goal is to explore the application and develop knowledge around the architecture and its workflow. 
It’s also important to learn how to navigate the most common observability
dashboards and move between LocalNet and DevNet.

The most direct update strategy in this phase is to regularly update
your local copy of the CN Quickstart by making a git pull from the main branch.

# Initial setup

::

   git clone https://github.com/digital-asset/cn-quickstart.git

   cd cn-quickstart

   # Regular updates during learning

   git pull origin main

   # Environment customization (only if needed)

   echo 'export PARTY_HINT="company-name"' > .envrc.private

   direnv allow

Experimentation phase
---------------------

(1-2 weeks)

In this phase, you’ll reinforce your understanding of the CN Quickstart by experimenting with the configurations, 
exploring the Ledger and CN app APIs, and modify the Daml code, Java client, and Makefile to test integration patterns.

At this phase, you may want to establish upstream tracking to selectively incorporate changes.

::

   # Set up upstream tracking

   git remote add upstream
   https://github.com/digital-asset/cn-quickstart.git

   # Create a branch for experiments

   git checkout -b experiments

   # Periodically incorporate upstream changes

   git fetch upstream

   git merge upstream/main

Development phase
-----------------

(2-3 weeks)

This is where you begin building your own application alongside the CN Quickstart sample application. 
Many developers create their new app in parallel code directories to the CN Quickstart application 
to learn from the CN Quickstart while building their own application.

::

   cn-quickstart/

   ├── quickstart/    # Original code
   │ ├── daml/        # Original Daml code
   │ ├── backend/     # Original backend service
   │ └── frontend/    # Original frontend
   │
   └── myapp/         # Your application code
     ├── daml/        # Your Daml models
     ├── backend/     # Your backend services
     └── frontend/    # Your frontend code

Developers may generate new Daml packages, new client code in languages
other than Java or TypeScript, UI elements, CI/CD integration, and unit
tests.

Gradle settings
~~~~~~~~~~~~~~~

When you develop parallel directories, remember to update your build
configuration to include both structures.

::

   // In settings.gradle.kts
   include("quickstart:daml")
   include("quickstart:backend")
   include("quickstart:frontend")
   include("myapp:daml")
   include("myapp:backend")
   include("myapp:frontend")

Maintain separate build files for application components.

::

   // In myapp/backend/build.gradle.kts
   dependencies {
   // Reference CN Quickstart components if needed
   implementation(project(":quickstart:daml"))

   // Your specific dependencies
   implementation("your.dependency:library:1.0.0")

   }

Environment variables
~~~~~~~~~~~~~~~~~~~~~

Use ``.envrc.private`` for local overrides.

::

   # Override CN Quickstart defaults

   export PARTY_HINT="your-company"

   export DAML_SDK_VERSION="your-version"

   # Add your application-specific variables

   export MY_APP_CONFIG="/path/to/config"

   Create separate environment files for your application.

   # In myapp/.env

   MY_APP_PORT=8080

   MY_APP_DB_URL=jdbc:postgresql://localhost:5432/myapp

Docker compose
~~~~~~~~~~~~~~

Create custom compose files that extend the CN Quickstart configuration.

::

   # In myapp/compose.yaml

   version: '3.8'

   # Import the CN Quickstart services

   include:

   - ../quickstart/compose.yaml

   # Add your services

   services:

   myapp-backend:

   build: ./backend

   depends_on:

   - postgres

   - participant

   environment:

   - DB_URL=${MY_APP_DB_URL}

   Use profiles to selectively enable groups of services.

   # Start with CN Quickstart and your services

   docker-compose --profile quickstart --profile myapp up

   # Start only your services (once they are able to run independently)

   docker-compose --profile myapp up

Separation phase
----------------

Over the course of a few weeks, CN developers have gained enough
experience and their new application’s complexity begins to exceed that
of the CN Quickstart. At this point, the CN Quickstart is no longer helpful and the
developer is advised to cut ties with the sample application.

To remove dependence on the CN Quickstart, delete the example application
directories, adjust gradle files, change the environment variable files,
and remove the upstream connection in git.

The developer’s source code repository is disconnected from the CN Quickstart
repository. It’s advisable to write a bridge document that maps
application components to their origins in the CN Quickstart to create a
historical development record.

::

   # Remove the CN Quickstart remote
   git remote remove upstream

   # Clean up unused directories (after backing up if needed)
   rm -rf quickstart/

   # Update build files to remove CN Quickstart references

   # Edit settings.gradle.kts, build.gradle.kts, etc.

Ongoing updates
---------------

By now, your application has likely outgrown the capabilities of the CN Quickstart.
However, the CN Quickstart continuously adds tooling features and updates existing tool versions. 
So you'll want the ability to update the development tooling and LocalNet support.

As a result, you'll periodically check into CN Quickstart to review the ChangeLog to see what is new, and then select components you’d like to include in your application.
You’ll find the CN Quickstart to be a source for improvements, rather than a direct dependency.

We recommend establishing a regular schedule (monthly or quarterly) to review CN Quickstart updates.

Your update strategy may include creating a temporary clone of the CN Quickstart to review changes, manually incorporating them into your project, and then removing the temporary clone.

::

   # Temporary clone to review changes

   git clone https://github.com/digital-asset/cn-quickstart.git
   cn-quickstart-temp

   cd cn-quickstart-temp

   git log --since="3 months ago" --pretty=format:"%h - %an, %ar : %s"

   # After identifying useful changes, manually incorporate them into your project

   # Then remove the temporary clone

   cd ..

   rm -rf cn-quickstart-temp

Every development team’s journey is unique. 
Adapt these strategies to fit your specific needs, team structure, and application requirements.
As a CN developer, your goal is to find an approach that supports your development goals while using the CN Quickstart as a foundation to accelerate your development lifecycle.
