Before do |scenario|
    # todo; by default, use all the initial tapes
    scenarioName = scenario.name.gsub(', ', '/').gsub(' ', '_')
    step "Start 2m test named #{scenarioName} using tapes: ##All"
    @driver.start_driver
end

After do |scenario|
  step "Stop test"
end

at_exit do
  response = Mimik.Stop(true, nil)
  finalized = response['finalized'] || '0'
  puts "Finalized #{finalized} test(s)" if finalized != '0'

  puts("Automation test End")
  @driver.x # Quit the driver and Pry
end
