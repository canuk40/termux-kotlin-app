#!/data/data/com.termux/files/usr/bin/bash
# Termux Desktop Environment Installation Script
# This script installs XFCE4 desktop environment with VNC server

set -e

DESKTOP_ENV="${1:-xfce4}"
INSTALL_APPS="${2:-true}"
INSTALL_FONTS="${3:-true}"

echo "====================================="
echo "Termux Desktop Environment Installer"
echo "====================================="
echo "Desktop: $DESKTOP_ENV"
echo "Apps: $INSTALL_APPS"
echo "Fonts: $INSTALL_FONTS"
echo "====================================="

# Update package lists
echo "→ Updating package lists..."
apt update -y

# Upgrade existing packages
echo "→ Upgrading packages..."
apt upgrade -y

# Install X11 repository
echo "→ Installing X11 repository..."
apt install -y x11-repo

# Update again with X11 repo
apt update -y

# Install VNC server and dependencies
echo "→ Installing VNC server..."
apt install -y tigervnc dbus

# Install desktop environment
echo "→ Installing desktop environment: $DESKTOP_ENV..."
case "$DESKTOP_ENV" in
    xfce4)
        apt install -y xfce4 xfce4-terminal xfce4-settings xfce4-goodies
        ;;
    lxqt)
        apt install -y lxqt
        ;;
    lxde)
        apt install -y lxde
        ;;
    fluxbox)
        apt install -y fluxbox
        ;;
    openbox)
        apt install -y openbox
        ;;
    *)
        echo "Unknown desktop environment: $DESKTOP_ENV"
        exit 1
        ;;
esac

# Install additional applications
if [ "$INSTALL_APPS" = "true" ]; then
    echo "→ Installing applications..."
    apt install -y \
        firefox \
        mousepad \
        thunar \
        thunar-archive-plugin \
        ristretto \
        file-roller \
        htop \
        git \
        neofetch || true  # Don't fail if some packages unavailable
fi

# Install fonts
if [ "$INSTALL_FONTS" = "true" ]; then
    echo "→ Installing fonts..."
    apt install -y fonts-noto fonts-noto-cjk || true
fi

# Create VNC directory
mkdir -p ~/.vnc
chmod 700 ~/.vnc

# Create VNC startup script
echo "→ Creating VNC startup script..."
cat > ~/.vnc/xstartup << 'XSTARTUP_EOF'
#!/data/data/com.termux/files/usr/bin/bash
export DISPLAY=:1
export PULSE_SERVER=127.0.0.1

# Start D-Bus
if [ -z "$DBUS_SESSION_BUS_ADDRESS" ]; then
    eval $(dbus-launch --sh-syntax)
fi

# Start desktop environment
case "DESKTOP_ENV_PLACEHOLDER" in
    xfce4)
        xfce4-session &
        ;;
    lxqt)
        startlxqt &
        ;;
    lxde)
        startlxde &
        ;;
    fluxbox)
        fluxbox &
        ;;
    openbox)
        openbox-session &
        ;;
esac
XSTARTUP_EOF

# Replace placeholder
sed -i "s/DESKTOP_ENV_PLACEHOLDER/$DESKTOP_ENV/g" ~/.vnc/xstartup
chmod +x ~/.vnc/xstartup

# Create VNC config
cat > ~/.vnc/config << 'CONFIG_EOF'
geometry=1280x720
localhost
alwaysshared
SecurityTypes=None
CONFIG_EOF

# Create launcher scripts
echo "→ Creating launcher scripts..."

# Start desktop script
cat > $PREFIX/bin/start-desktop << 'START_EOF'
#!/data/data/com.termux/files/usr/bin/bash
# Termux Desktop Launcher

# Kill existing VNC servers
vncserver -kill :1 2>/dev/null || true
sleep 1

# Start VNC server
echo "Starting desktop environment..."
vncserver :1 \
    -localhost yes \
    -geometry 1280x720 \
    -depth 24 \
    -dpi 96

if [ $? -eq 0 ]; then
    echo "✓ Desktop started successfully!"
    echo "  Display: :1"
    echo "  Port: 5901"
    echo "  Connection: localhost:5901"
else
    echo "✗ Failed to start desktop"
    exit 1
fi
START_EOF
chmod +x $PREFIX/bin/start-desktop

# Stop desktop script
cat > $PREFIX/bin/stop-desktop << 'STOP_EOF'
#!/data/data/com.termux/files/usr/bin/bash
# Stop Termux Desktop

echo "Stopping desktop environment..."
vncserver -kill :1 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✓ Desktop stopped"
else
    echo "No desktop session running"
fi
STOP_EOF
chmod +x $PREFIX/bin/stop-desktop

# List sessions script
cat > $PREFIX/bin/list-desktop << 'LIST_EOF'
#!/data/data/com.termux/files/usr/bin/bash
# List VNC sessions

echo "Active VNC sessions:"
vncserver -list
LIST_EOF
chmod +x $PREFIX/bin/list-desktop

# Configure XFCE4 for touch if applicable
if [ "$DESKTOP_ENV" = "xfce4" ]; then
    echo "→ Configuring XFCE4 for touch..."
    mkdir -p ~/.config/xfce4/xfconf/xfce-perchannel-xml
    
    cat > ~/.config/xfce4/xfconf/xfce-perchannel-xml/xfwm4.xml << 'XFWM4_EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfwm4" version="1.0">
  <property name="general" type="empty">
    <property name="theme" type="string" value="Default"/>
    <property name="title_font" type="string" value="Sans Bold 14"/>
    <property name="easy_click" type="bool" value="true"/>
    <property name="mousewheel_rollup" type="bool" value="false"/>
    <property name="use_compositing" type="bool" value="false"/>
    <property name="borderless_maximize" type="bool" value="true"/>
  </property>
</channel>
XFWM4_EOF
fi

# Create installation marker
echo "Installed: $(date)" > ~/.desktop_installed
echo "Environment: $DESKTOP_ENV" >> ~/.desktop_installed
echo "Version: 1.0" >> ~/.desktop_installed

echo ""
echo "====================================="
echo "✓ Installation complete!"
echo "====================================="
echo ""
echo "Commands:"
echo "  start-desktop  - Start desktop environment"
echo "  stop-desktop   - Stop desktop environment"
echo "  list-desktop   - List active sessions"
echo ""
echo "Manual control:"
echo "  vncserver :1           - Start VNC server"
echo "  vncserver -kill :1     - Stop VNC server"
echo "  vncserver -list        - List sessions"
echo ""
echo "Connect to: localhost:5901"
echo "====================================="
