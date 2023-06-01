package storeCombined

trait StoreClient {
  def get(key: String, action: Option[String] => Unit): Unit
  def set(key: String, value: String): Unit
}
