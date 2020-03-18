# frozen_string_literal: true

require 'uri'
require 'net/http'
require 'json'

class String
  def is_integer?
    !!(self.match(/\d+/))
  end

  def as_Int
    mat = self.match(/(\d+)/)
    if mat.nil?
      nil
    else
      mat[1].to_i
    end
  end

  def trim
    strip! || self
  end
end

class Array
  def exclude?(object)
    !include?(object)
  end

  def contains?(test)
    downcased = Array.new self.map(&:downcase)
    downcased.include? test.to_s.downcase
  end

  def contains_all?(tests)
    downcased_in = Array.new self.map(&:downcase)
    downcased_t = Set.new tests.map(&:downcase)
    downcased_t.all?{|f| downcased_in.include? f.to_s.downcase}
  end
end


# Start test using tapes: xx,yy,zz
# Start 8s test using tapes: xx,yy,zz
# Start 3m test named aaa using tapes: xx, yy,  zz
Given(/Start (?:(\d+\w) )?test (?:named (.+) )?using tapes:(.+)$/) do |time, handle, items|
  response = Mimik.Start(time || '', handle || '', items: items || '')

  case response.code
  when '200'
    $rTests << handle

  when '201'
    $rTests << response['handle']

  when '400'
    puts 'Invalid configuration, please see Mimik logs'

  when '412'
    puts 'Mimik has no tapes'

  when '503'
    puts 'Mimik is not running'
  end

  expect(response.code).to match(/2.+/)
end

Then(/Append tapes to the current test:(.+)$/) do |items|
  response = Mimik.Append(items)

  puts "Code: #{response.code.inspect}"
  case response.code
  when '304'
    puts 'Request -> No change'
  when '400'
    puts 'Invalid configuration, please see Mimik logs'
  when '503'
    puts 'Mimik is not running'
  end
  expect(response.code).not_to eql('400')
end

Then(/Disable tapes in the current test:(.+)$/) do |items|
  response = Mimik.Disable(items)

  puts "Code: #{response.code.inspect}"
  case response.code
  when '304'
    puts 'Request -> No change'
  when '400'
    puts 'Invalid configuration, please see Mimik logs'
  when '503'
    puts 'Mimik is not running'
  end
  expect(response.code).not_to eql('400')
end

# Stop test
# - stops the last started test
# Stop all tests
# Stop test: xxx
# Stop tests: xxx, yyy
Then(/Stop (all)? ?tests?(?::(.+))?$/) do |all, items|
  response = Mimik.Stop(all, items)

  unless response.nil?
    case response.code
    when '304'
      puts "Mimik - #{response['missing']} test(s) were not found/ stopped"
    when '400'
      puts 'Invalid configuration, please see Mimik logs'
    when '503'
      puts 'Mimik is not running'
    end

    rHeads = response.instance_variable_get('@header')
    rHeads.each do |rh|
      if rh[0].end_with?('_time')
        #        puts "(#{rh[0].chomp('_time')}) ran for #{rh[1][0]}"
        puts "Test ran for #{rh[1][0]}"
      end
    end
  end
end

# | Condition | Source  | S.Type | S.Index | S.Match | Action (var) | Action (match) |
# or
# | Seq | Condition | Source  | S.Type | S.Index | S.Match | Action (var) | Action (match) |
#Given(/In the mock (.+), apply the following sequences?/) do |chapter, table|
Given('In the mock {string}, apply the following sequences:') do |chapter, table|
   expect(table.is_a?(Cucumber::MultilineArgument::DataTable)).to be_truthy

   seqTable = table.raw # DataTable -> [[...],[...]]
   expect(MimikSeq.isValid?(seqTable)).to be_truthy

   seqTable = MimikSeq.ensureOrder(seqTable) # with given data, ensure in order
   seqTable = MimikSeq.ensureData(seqTable) # add missing columns
   seqTable = MimikSeq.ensureSeq(seqTable) # ensure valid sequences
   seqTable = seqTable.drop(1) # don't need headers anymore

   result = MimikSeq.parseData(seqTable)
   response = Mimik.Modify(chapter, result)
   expect(response.code).to eql('200')
end

class MimikSeq
  attr_accessor :seq
  attr_accessor :cond
  attr_accessor :source, :sType, :sIndex, :sMatch
  attr_accessor :aVar, :aMatch

  def initialize(data)
    @seq = data[0]
    @cond = data[1]
    @source = data[2]
    @sType = data[3]
    @sIndex = data[4]
    @sMatch = data[5]
    @aVar = data[6]
    @aMatch = data[7]
  end

  def to_s()
    sItems = [@source]
    sItems += [':', @sType] if !@sType.empty?
    sItems += ['[', @sIndex, ']'] if !@sIndex.empty?
    sItems += [':{', @sMatch, '}'] if !@sMatch.empty?

    aItems = []
    if ( !@aVar.empty? || !@aMatch.empty? )
      aItems += ['->']
      aItems += [@aVar] if !@aVar.empty?
      aItems += [':{', @aMatch, '}'] if !@aMatch.empty?
    end

    ([@seq, '_', @cond] + sItems + aItems).join()
  end

  class << self
    # 0) In general, the data must have [source s.stpe] or [source s.match]
    def isValid?(data) #[[heads], [..]]
      va = %w[source s.type]
      vb = %w[source s.match]
      data[0].contains_all?(va) || data[0].contains_all?(vb)
    end

    # 1) Adjusts the incoming data so columns are in the proper order
    def ensureOrder(data)
      heads = %w[Seq Condition Source S.Type S.Index S.Match Action\ (var) Action\ (match)]

      outData = []
      data.size.times{ outData.push(Array.new) }

      heads.each_with_index{ |val, _|
        if data[0].contains? val
          col = data[0].get_index(val)
          data.each_with_index{ |row, rIdx|
            outData[rIdx].push(row[col])
          }
        end
      }
      outData
    end

    # 2) Appends the missing (optional) columns)
    def ensureData(data)
      ckHeads = {}
      ckHeads['Seq'] = 0
      ckHeads['Condition'] = 1
      ckHeads['S.Type'] = 3
      ckHeads['S.Index'] = 4
      ckHeads['S.Match'] = 5
      ckHeads['Action (var)'] = 6
      ckHeads['Action (match)'] = 7

      ckHeads.each{ |cK, cV|
        # do nothing, unless the data is missing
        unless data[0].contains?(cK)
          data[0] = data[0].insert(cV, cK)
          data.drop(1).each_with_index { |val, idx|
            data[idx+1] = val.insert(cV, '')
          }
        end
      }
    end

    # 3) Append missing sequence numbers, as every item needs one
    def ensureSeq(data)
      seqCnt = '0'
      pSeq = 0
      data.map{ |row|
        if !seqCnt.is_Integer?
          "" # previous row was invalid,
        elsif row[0].empty?
          [seqCnt] + row.drop(1) # | cond | .... |
        else
          seqCnt = row[0] # | seq # | .... |
          if seqCnt.is_Integer?
            if seqCnt.to_i < pSeq
              "" # seq must be in order if numbered
            else
              pSeq = seqCnt.to_i
              row
            end
          else # row is invalid seq type, ignore following rows
            ""
          end
        end
      }
      .keep_if{|d| d.is_a? Array} #ignore all the invalid data
    end

    # 4) Converts string arrays to MimikSeq arrays
    def parseData(data)
      outData = []
      if data.is_a? Array # [???]
        return if data.empty?
        if data[0].is_a? Array #[[...], [...]]
          data.each{ |d|
            outData += [MimikSeq.new(d)]
          }
        else # [...]
          outData += [MimikSeq.new(data)]
        end
      end
      outData
    end

  end # end << self
end

class Mimik
  TestingUrl = 'http://0.0.0.0:4321/tests'

  def self.isMocksBuild
    configatron.flavor == "mock"
  end

  def self.isMimikOn
    return Net::HTTPResponse::CODE_TO_OBJ['200'].new(nil, '200', nil) if !isMocksBuild

    uri = URI.parse('http://0.0.0.0:2202/')
    req = Net::HTTP::Post.new(uri)

    begin
      http = Net::HTTP.new(uri.host, uri.port)
      http.request(req)
      Net::HTTPResponse::CODE_TO_OBJ['200'].new(nil, '200', nil)
    rescue Errno::ECONNREFUSED, Timeout::Error, SocketError
      Net::HTTPResponse::CODE_TO_OBJ['503'].new(nil, '503', nil)
    end
  end


  def self.Start(time, handle, items:)
    return isMimikOn unless isMimikOn.code == '200'

    $rTests = $rTests || []
    time ||= ''
    handle ||= ''

    tapes = (items || '').gsub(/\s+/, '')

    uri = URI.parse("#{TestingUrl}/start")

    req = Net::HTTP::Post.new(uri)
    req['time'] = time
    req['handle'] = handle
    req['tape'] = tapes
    response = doRequest(req)

    # prepare a list of handles to finalize
    $sTests = [] if $sTests.nil?
    $sTests << response['handle'] unless response['handle'].nil?

    response
  end

  def self.Append(items)
    return isMimikOn unless isMimikOn.code == '200'

    if $rTests.nil? || $rTests.empty?
      Start(items: items)
    else
      tapes = items.gsub(/\s+/, '')
      uri = URI.parse("#{TestingUrl}/append")

      req = Net::HTTP::Post.new(uri)
      req['handle'] = $rTests.last || ''
      req['tape'] = tapes
      doRequest(req)
    end
  end

  def self.Disable(items)
    return isMimikOn unless isMimikOn.code == '200'

    if !$rTests.nil? && !$rTests.empty?
      tapes = items.gsub(/\s+/, '')
      uri = URI.parse("#{TestingUrl}/disable")

      req = Net::HTTP::Post.new(uri)
      req['handle'] = $rTests.last || ''
      req['tape'] = tapes
      doRequest(req)
    end
  end

  def self.Stop(all, items)
    return isMimikOn unless isMimikOn.code == '200'

    $rTests = [] if $rTests.nil?
    handles = [items.gsub(/\s+/, '')] unless items.nil?

    # when "all" is set, finalize any
    if all && !$sTests.nil? && !$sTests.empty?
      handles = $sTests
      $sTests = []
      handles << '##Finalize'
    end

    # no items were given, and "all" is false, so use the last started test
    handles = [$rTests.pop] if handles.nil? && !$rTests.empty?

    if handles.nil? || handles.empty?
      # puts "No tests running to stop"
      Net::HTTPResponse::CODE_TO_OBJ['200'].new(nil, '200', nil)
    else
      $rTests -= handles

      uri = URI.parse("#{TestingUrl}/stop")
      req = Net::HTTP::Post.new(uri)
      req['handle'] = handles
      doRequest(req)
    end
  end

  def self.Modify(chapter, actions)
    return isMimikOn unless isMimikOn.code == '200'

    outData = {}
    outData[chapter] = actions
      .map{ |m| # `##_......"
        q = m.to_s.split('_') # `##_...` -> `[##, "...", ...]
        [q[0], q.drop(1).join()] # `[##, "..."]
      }
      .group_by{ |g| g[0] }
      .map{ |_, mV|
        mV.map{ |d| d.drop(1)[0] }
      }

    uri = URI.parse("#{TestingUrl}/modify")
    req = Net::HTTP::Post.new(uri)
    req.body = outData.to_json
    req['handle'] = $rTests.last || ''
    doRequest(req)
  end

  private

  def self.doRequest(request)
    unless request.nil?
      begin
        http = Net::HTTP.new(request.uri.host, request.uri.port)
        http.request(request)
      rescue Errno::ECONNREFUSED, Timeout::Error, SocketError
        Net::HTTPResponse::CODE_TO_OBJ['503'].new(nil, '503', nil)
      end
    end
  end
end
