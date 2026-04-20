# Vagrantfile for local development environment

This Vagrantfile sets up a local development environment for by creating a
virtual machine with Nix, direnv and a few other tools pre-installed.

## Requirements

- [Vagrant](https://www.vagrantup.com/downloads)
- [VirtualBox](https://www.virtualbox.org/wiki/Downloads)

## Start

```shell
vagrant up
```

## Use

```shell
vagrant ssh
cd cn-quickstart
```

## Stop

```shell
vagrant halt
```

## Clean up

```shell
vagrant destroy

# Optionally, remove the Nix cache to free up space, this will make the next
# `vagrant up` slower as it will need to re-download all Nix packages.
rm -r vagrant-nix-cache
```

## Repopulating Nix cache without recreating the VM

```shell
vagrant halt
rm -r vagrant-nix-cache
vagrant up --provision
```

## Notes

- Only Ubuntu 24.04 is tested as a host OS however other Linux distributions
  and Intel-based macOS should work as well.
- Vagrant creates the `vagrant-nix-cache` directory with `nix-cache.img` file
  which is shared with the VM. This file is used to store /nix/cache and
  /nix/var/nix/db for faster builds and installations of Nix packages. The size
  of the image can be changed in the [Vagrantfile](Vagrantfile).
- IP of the VM is set to `192.168.56.10` (it can be changed in the
  [Vagrantfile](Vagrantfile)).
- The VM host can be accessed using [nip.io](https://nip.io) domain names, for
  example:
  - 192.168.56.10.nip.io
  - 192-168-56-10.nip.io
  - myapp.192-168-56-10.nip.io
- Alternatively you can configure port forwarding to localhost by adjusting
  `ports_to_forward` in the [Vagrantfile](Vagrantfile).
