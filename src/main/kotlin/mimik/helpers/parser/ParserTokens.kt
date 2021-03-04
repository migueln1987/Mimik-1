package mimik.helpers.parser

enum class ParserTokens(val flag: String) {
    // === Conditional group
    /**
     * A conditional requirement is specified this command.
     *
     * Required for all conditionals
     * @see cond
     */
    cond("cond"),

    /**
     * Marks this command as Optional for running an action.
     * Further commands after this one are always run regardless of source result
     *
     * When combined with "?" or "!"
     * - "~?" = Action will only run when source matching is 'true'
     * - "~!" = Action will only run when source matching is 'false'
     * - "~" = Previous command must be a "~"; this source has the same cond as prior command
     * @see cP
     */
    cond_opt("cP"),

    /** Source must match to be "successful"
     * @see cT
     */
    cond_true("cT"),

    /**
     * Source must NOT match to be "successful"
     * @see cF
     */
    cond_false("cF"),
    // === Conditional group end

    // === Source group
    /**
     * Flag for section defined as "source".
     *
     * Required for ALL valid commands
     * @see source
     */
    source("source"),

    /** Source is a Request or Response
     * @see rType
     */
    type_RX("rType"),

    /** Use the Request data
     * @see rIn
     */
    rx_In("rIn"),

    /** Use the Request Header data
     * @see rInH
     */
    req_Header("rInH"),

    /** Request header by name
     * @see rInHN
     */
    req_HeadName("rInHN"),

    /** Use the Request Body data
     * @see rInB
     */
    req_Body("rInB"),

    /** Use the Response data
     * @see rOut
     */
    rx_Out("rOut"),

    /** Use the Request Header data
     * @see rOutH
     */
    res_Header("rOutH"),

    /** Response header by name
     * @see rOutHN
     */
    res_HeadName("rOutHN"),

    /**
     * Use the Request Body data
     * @see rOutB
     */
    res_Body("rOutB"),

    /**
     * Regex Match for Request or Response items
     * @see rM
     */
    rx_Match("rM"),

    /** Source is a variable
     * @see vType
     */
    type_Var("vType"),

    /** Flag marking that scope bounds were defined
     * @see vbound
     */
    var_bound("vbound"),

    /** Source variable is at the Chapter scope level
     * @see vC
     */
    var_Chap("vC"),

    /** Source variable is at the Test Bounds scope level
     * @see vB
     */
    var_Bounds("vB"),

    /**
     * Higher scopes may be searched for a matching variable
     *
     * Usage
     * - ^Name
     *    - Search: Self, Chapter, Bounds
     * - &^Name
     *    - Search: Chapter, Bounds
     * - %^Name
     *    - Search: Bounds
     *    - Same as "%Name"
     * @see vU
     */
    var_UpSrc("vU"),

    /** Variable is being referenced by literal name
     * @see vN
     */
    var_Name("vN"),

    /** Variable is being referenced by regex matching
     * @see vM
     */
    var_Match("vM"),

    /** Source is the "use" variable in the chapter
     * @see uType
     */
    type_Use("uType"),

    /** Collecting Use value from another chapter
     * @see uN
     */
    use_Name("uN"),

    /** Regex action (math/ logic) on the use value
     * @see uM
     */
    use_Match("uM"),
    // === Source group end

    // === Act group
    /** Command contains an action.
     *
     * Required for all Actions
     * @see act
     */
    action("act"),

    /** Action outputs to a variable. Required for all actions
     * @see aV
     */
    act_var("aV"),

    /** An Action variable Level is defined
     * @see aSVL
     */
    act_level("aSVL"),

    /** Store the variable at the Chapter's scope level
     * @see aSC
     */
    act_lvChap("aSC"),

    /** Store the variable at the Test Bound's scope level
     * @see aSB
     */
    act_lvBound("aSB"),

    /** Name which to store the variable as
     * @see aVN
     */
    act_Name("aVN"),

    /** What kind of additional variables to create
     * - act_Exist
     * - act_Count
     * - act_CondRst
     * - act_IdxSprd
     *    - act_IdxSAll
     *    - act_IdxSI
     *    - act_IdxSL
     *
     * @see aVT
     */
    act_VarTypes("aVT"),

    /**
     * Saves the result of "does the source match exist"
     *
     * @param Input Name?
     * @param Result Variable?
     * @see aVE
     */
    act_Exist("aVE"),

    /**
     * Saves the result of "how many items were matched"
     *
     * @param Input Name#
     * @param Result Variable#
     * @see aVC
     */
    act_Count("aVC"),

    /**
     * Saves the result of "Result of the conditional matching"
     *
     * @param Input Name@
     * @param Result Variable@
     * @see aVR
     */
    act_CondRst("aVR"),

    /**
     * Saves the result based on the index
     *
     * @param Style Name_...
     * @see aVS
     */
    act_IdxSprd("aVS"),

    /**
     * Saves the matched results to individual indexes
     *
     * @param Input Name_#
     * @param Result Variable_0, Variable_1, Variable_2, etc.
     *
     * @see aVSA
     */
    act_IdxSAll("aVSA"),

    /**
     * Saves only the specified matched index.
     *
     * @param Input Name_000 (where "000" is the chosen index, 0 -> 9999)
     * @param Result Variable_0 -> Variable_9999
     *
     * @see aVSI
     */
    act_IdxSI("aVSI"),

    /**
     * Saves the last matched index.
     *
     * @param Input Name_?
     * @param Result Variable_?
     *
     * @see aVSL
     */
    act_IdxSL("aVSL"),

    /** Action de-templates the matching content, then updates the source
     * @see aM
     */
    act_Match("aM");

    override fun toString(): String = flag
}
