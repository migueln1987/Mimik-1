
Given('Input table via a ruby step') do
  tb = [
    %w[Type From To],
    ['Body', 'workflowCode.+?\d+\"', "workflowCode\": \"#{workflow_code}\""]
  ]

  tb = [

  ]
  step 'In the response "FRAUD_CASE_INFO", apply the following replacements:', table(tb)
end

