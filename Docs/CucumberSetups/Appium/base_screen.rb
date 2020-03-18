# frozen_string_literal: true

class BaseScreen < PageFactory
  class << self
    # Operation system
    configatron.os = ENV['OS'] || 'android'

    # Device Name
    devices = `$ANDROID_HOME/platform-tools/adb devices | awk -F emulator "NF > 1"`
    if !devices.empty?
      configatron.devicename = 'Android Emulator'
      configatron.avd = nil
      device = devices.split(' ')[0]
      puts "Devices: #{device}"
      configatron.version = version = `$ANDROID_HOME/platform-tools/adb -P 5037 -s #{device} shell getprop ro.build.version.release`
                                      .gsub(/\n/, '')
    else
      puts "no emulator found, checking for AVD ('pixel_xl' or DeviceName input)"
      configatron.devicename = ENV['DeviceName'] || 'pixel_xl'
      configatron.avd =  ENV['DeviceAVD'] || configatron.devicename
      configatron.version = ENV['DeviceVersion'] || '9'
    end

    # Build Name (debug, release, externalNativeBuild)
    configatron.build = ENV['Build'] || 'debug'
    puts "Build: #{configatron.build}"

    # Build Flavor (demo, mock, prod, dev)
    configatron.flavor = ENV['Flavor'] || 'demo'
    puts "Flavor: #{configatron.flavor}"
  end
end
