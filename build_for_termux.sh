#!/bin/bash

echo "Setting up Android development environment in Termux..."

# Install required packages
pkg install -y openjdk-17 wget zip unzip

# Download Android SDK command line tools
if [ ! -d "$HOME/android-sdk" ]; then
    mkdir -p $HOME/android-sdk
    cd $HOME/android-sdk
    wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
    unzip commandlinetools-linux-9477386_latest.zip
    mkdir -p cmdline-tools/latest
    mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
fi

# Set environment variables
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Accept licenses
yes | sdkmanager --licenses

# Install required SDK components
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "Environment setup complete!"
echo "Now navigate to your project directory and run:"
echo "  ./gradlew build"
