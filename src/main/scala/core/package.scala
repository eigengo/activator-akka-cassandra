package object core {

  type ErrorMessage = String

  private[core] object Keyspaces {
    val akkaCassandra = "akkacassandra"
  }

  private[core] object ColumnFamilies {
    val tweets = "tweets"
  }

}
