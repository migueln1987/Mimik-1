$: << File.dirname(__FILE__) + "/../../lib"

require "appium_lib"
require "csv"
require "json"

apk_name = "app-%s-%s.apk" % [configatron.flavor, configatron.build]
apk_path = "app/build/outputs/apk/%s/%s" % [configatron.flavor, configatron.build]
app_location = File.absolute_path((File.join(File.dirname(__FILE__), "../../", apk_path, apk_name)))

opts = {
  caps: {
    platformName: 'Android',
    automationName: 'UiAutomator2',
    platformVersion: configatron.version,
    deviceName: configatron.devicename,
    avd: configatron.avd,
    app: app_location,
    noReset: true,
    fullReset: false,
    avdArgs: '-no-snapshot-load',
    newCommandTimeout: 3600,
    autoGrantPermissions: true
  },
  appium_lib: {
    wait: 0,
    debug: false
  }
}

@driver = Appium::Driver.new(opts, true)
Appium.promote_appium_methods Object
