library(RCassandra)
library(plyr)
library(stringr)
library(ggplot2)


sentiment.score = function(tweets, pos.words, neg.words, .progress='none') {
    # Parameters
    # tweets: vector of tweet bodies to score
    # pos.words: vector of words of postive sentiment
    # neg.words: vector of words of negative sentiment
    # .progress: passed to laply() to control of progress bar

    scores = laply(tweets, function(sentence, pos.words, neg.words) {
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
    scores.df = data.frame(text=tweets, score=scores)
    return(scores.df)
}

sentiment.main = function() {
	c <- RC.connect('localhost', '9160')
	RC.use(c, 'akkacassandra')

	pos <- RC.get.range.slices(c, "positivewords",    first="word", last="word", fixed=TRUE, comparator='UTF8Type')$word
	neg <- RC.get.range.slices(c, "negativewords",    first="word", last="word", fixed=TRUE, comparator='UTF8Type')$word
	tweets <- RC.get.range.slices(c, "tweets", first="text", last="text", fixed=TRUE, comparator='UTF8Type')$text
	
	RC.close(c)

	scores = sentiment.score(tweets, pos, neg, .progress='text')
    nd = c(length(tweets))

    # add variables to data frame
    scores$all = factor(rep(c("tweets"), nd))
    scores$very.pos = as.numeric(scores$score >= 2)
    scores$very.neg = as.numeric(scores$score <= -2)

    # how many very positives and very negatives
    numpos = sum(scores$very.pos)
    numneg = sum(scores$very.neg)

    # global score
    global_score = round( 100 * numpos / (numpos + numneg) )

    # colors
    cols = c("#7CAE00", "#00BFC4", "#F8766D", "#C77CFF")
    names(cols) = c("tweets")

    # boxplot
    ggplot(scores,
        aes(x=all, y=score, group=all)) +
        geom_boxplot(aes(fill=all)) +
        scale_fill_manual(values=cols) +
        geom_jitter(colour="gray40",
        position=position_jitter(width=0.2), alpha=0.3) +
    opts(title = "Tweet mood")
}
