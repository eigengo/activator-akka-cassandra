library("RCassandra")

# function score.sentiment
sentiment.score = function(sentences, pos.words, neg.words, .progress='none') {
    # Parameters
    # sentences: vector of text to score
    # pos.words: vector of words of postive sentiment
    # neg.words: vector of words of negative sentiment
    # .progress: passed to laply() to control of progress bar

    scores = laply(sentences, function(sentence, pos.words, neg.words) {
        # remove punctuation
        sentence = gsub("[[:punct:]]", "", sentence)
        # remove control characters
        sentence = gsub("[[:cntrl:]]", "", sentence)
        # remove digits?
        sentence = gsub('\\d+', '', sentence)

        # define error handling function when trying tolower
        tryTolower = function(x) {
            # create missing value
            y = NA
            # tryCatch error
            try_error = tryCatch(tolower(x), error=function(e) e)
            # if not an error
            if (!inherits(try_error, "error"))
            y = tolower(x)
            # result
            return(y)
        }
        # use tryTolower with sapply 
        sentence = sapply(sentence, tryTolower)

        # split sentence into words with str_split (stringr package)
        word.list = str_split(sentence, "\\s+")
        words = unlist(word.list)

        # compare words to the dictionaries of positive & negative terms
        pos.matches = match(words, pos.words)
        neg.matches = match(words, neg.words)

        # get the position of the matched term or NA
        # we just want a TRUE/FALSE
        pos.matches = !is.na(pos.matches)
        neg.matches = !is.na(neg.matches)

        # final score
        score = sum(pos.matches) - sum(neg.matches)
        return(score)
        }, pos.words, neg.words, .progress=.progress )

    # data frame with scores for each sentence
    scores.df = data.frame(text=sentences, score=scores)
    return(scores.df)
}

sentiment.main = function() {
	pos = readLines("positive_words.txt")
	neg = readLines("negative_words.txt")

	c <- RC.connect('localhost', '9160')
	RC.use(c, 'akkacassandra')
	r <- RC.get.range.slices(c, 'tweets', fixed=FALSE)
	
	RC.close(c)

	scores = score.sentiment(ari_txt, pos, neg, .progress='text')

}
