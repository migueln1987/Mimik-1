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

def ensureOrder(data)
  heads = %w[Seq Condition Source S.Type S.Index S.Match Action\ (var) Action\ (match)]

  outData = []
  data.size.times{ outData.push(Array.new) }
  puts outData.to_s

  heads.each_with_index{ |val, col|
    if data[0].contains? val
      data.each_with_index{ |row, rIdx|
        puts "Adding: `#{row[col]}` to column #{col}"
        outData[rIdx].push(row[col])
      }
      puts "Now:\n #{outData.to_s}"
    end
  }
  outData
end

aa = ["Seq", "S.Type", "Source", "S.Index", "S.Match", "Condition", "Action (var)", "Action (match)"]
ab = ["", "?", "request", "body", "", "", "", "aa_bb"]
ac = [aa, ab]

puts ac.to_s
puts ensureOrder(ac).to_s
