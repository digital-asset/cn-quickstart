# VM configuration
cpus   = 4 # Cores
memory = 12 # GiB
ipaddr = "192.168.56.10"

# List of ports to forward from the VM to localhost
ports_to_forward = [
  8082, # Keycloak
]

external_volume_size = 20 # GiB
external_volume_image_path = "/vagrant/.vagrant.volume.img"
external_volume_mount_path = "/external-volume"

shell_variables = <<~"SHELL"
  ROOT_DIR="/vagrant"
  EXTERNAL_VOLUME_SIZE="#{external_volume_size}"
  EXTERNAL_VOLUME_IMAGE_PATH="#{external_volume_image_path}"
  EXTERNAL_VOLUME_MOUNT_PATH="#{external_volume_mount_path}"
SHELL

shell_common = <<~'SHELL'
  set -euo pipefail
  trap exit_message EXIT
SHELL

shell_helpers = <<~'SHELL'
  exit_message () {
    if [[ $? -ne 0 ]]; then
      echo -e "The provisioning has been interrupted. Please try again:\n\n  vagrant up --provision\n " >&2
    fi
  }

  append_line_to_file () {
    local line="$1"
    local file="$2"

    grep -qxF "$line" "$file" || echo "$line" >> "$file"
  }
SHELL

Vagrant.configure("2") do |config|
  config.vm.box = "bento/ubuntu-26.04"

  config.vm.network "private_network", ip: ipaddr

  ports_to_forward.each do |port|
    config.vm.network "forwarded_port", guest: port, host_ip: "127.0.0.1", host: port
  end

  config.vm.provider "virtualbox" do |vb|
    vb.cpus = cpus
    vb.memory = memory * 1024
    vb.customize ["storagectl", :id, "--name", "VirtIO Controller", "--hostiocache", "on"]
  end

  config.vm.provision "shell", name: "system", upload_path: "/tmp/vagrant-shell-system", reset: true do |s|
    s.inline = shell_variables + shell_common + shell_helpers + <<~'SHELL'
      apt-get update

      apt-get install -y \
        bash-completion \
        command-not-found \
        git \
        direnv \
        nix \
        docker-compose-v2

      adduser vagrant nix-users
      adduser vagrant docker

      # Create a file to be mounted and used as a back store
      # This works more reliably than using a synced folder (vboxsf) directly
      [[ -d "$EXTERNAL_VOLUME_MOUNT_PATH" ]] || mkdir -p "$EXTERNAL_VOLUME_MOUNT_PATH"
      [[ -f "$EXTERNAL_VOLUME_IMAGE_PATH" ]] || { truncate -s "${EXTERNAL_VOLUME_SIZE}G" "$EXTERNAL_VOLUME_IMAGE_PATH"; mkfs.btrfs "$EXTERNAL_VOLUME_IMAGE_PATH"; }

      external_volume_data_image_fstab="$EXTERNAL_VOLUME_IMAGE_PATH $EXTERNAL_VOLUME_MOUNT_PATH btrfs loop,compress=zstd 0 0"
      append_line_to_file "$external_volume_data_image_fstab" /etc/fstab

      daemons_need_restart=false

      # Mount the external volume if not already mounted
      mountpoint -q "$EXTERNAL_VOLUME_MOUNT_PATH" || {
        mount "$EXTERNAL_VOLUME_MOUNT_PATH" || exit 1
        daemons_need_restart=true
      }

      # Bind mounts
      for path in /nix/store /nix/var/nix/db /var/lib/containerd; do
        mkdir -p "$EXTERNAL_VOLUME_MOUNT_PATH$path" "$path"
        fstab_entry="$EXTERNAL_VOLUME_MOUNT_PATH$path $path none bind 0 0"
        append_line_to_file "$fstab_entry" /etc/fstab
      done

      systemctl daemon-reload
      mount -a

      # Restart Daemons if needed
      if "$daemons_need_restart"; then
        systemctl restart nix-daemon.service
        systemctl restart containerd.service
      fi
    SHELL
  end

  config.vm.provision "shell", name: "user", upload_path: "/tmp/vagrant-shell-user", privileged: false do |s|
    s.inline = shell_variables + shell_common + shell_helpers + <<~'SHELL'
      # Configure nix
      mkdir -p ~/.config/nix/
      echo "extra-experimental-features = nix-command flakes" > ~/.config/nix/nix.conf

      # Enable direnv
      direnv_bash_hook='eval "$(direnv hook bash)"'
      append_line_to_file "$direnv_bash_hook" ~/.bashrc

      # Change to the cn-quickstart directory and allow direnv
      [[ -d "$HOME/cn-quickstart" ]] || ln -s "$ROOT_DIR" "$HOME/cn-quickstart"
      cd "$ROOT_DIR"
      direnv allow

      # Execute direnv export to set up the environment
      eval "$(direnv export bash)"
    SHELL
  end
end
