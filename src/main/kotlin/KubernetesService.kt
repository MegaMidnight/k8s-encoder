import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.ApiException
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.Config

class KubernetesService {
    @Throws(ApiException::class)
    fun getNodeCount(): Int {
        val client: ApiClient = Config.defaultClient()
        Configuration.setDefaultApiClient(client)
        val k8sApi = CoreV1Api()
        val nodeList = k8sApi.listNode().execute()
        val nodeCount = nodeList?.items?.size
        if (nodeCount == null || nodeCount <= 0) {
            throw IllegalStateException("Node count must be greater than 0")
        }
        return nodeCount
    }

    fun getNodes(): List<String?> {
        val client: ApiClient = Config.defaultClient()
        Configuration.setDefaultApiClient(client)
        val k8sApi = CoreV1Api()
        val nodeList = k8sApi.listNode().execute()
        return nodeList?.items?.map { it.metadata?.name } ?: emptyList()
    }

}