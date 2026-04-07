package com.instagram.features.search.application

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.slf4j.LoggerFactory
import java.util.UUID

data class UserIndexDto(
    val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String?
)

class SearchService(
    esHost: String = System.getProperty("ELASTICSEARCH_HOST") ?: System.getenv("ELASTICSEARCH_HOST") ?: "localhost",
    esPort: Int = System.getProperty("ELASTICSEARCH_PORT")?.toIntOrNull() ?: System.getenv("ELASTICSEARCH_PORT")?.toIntOrNull() ?: 9200
) {
    private val logger = LoggerFactory.getLogger(SearchService::class.java)
    private val client: ElasticsearchClient?

    init {
        client = try {
            val restClient = RestClient.builder(
                HttpHost(esHost, esPort, "http")
            ).build()
            
            val mapper = ObjectMapper().registerKotlinModule()
            val transport = RestClientTransport(restClient, JacksonJsonpMapper(mapper))
            ElasticsearchClient(transport)
        } catch (e: Exception) {
            logger.error("Failed to initialize Elasticsearch client. Search will be disabled.", e)
            null
        }
    }

    fun indexUser(user: UserIndexDto) {
        if (client == null) return
        
        try {
            val req = IndexRequest.of { b ->
                b.index("users")
                 .id(user.id)
                 .document(user)
            }
            client.index(req)
            logger.info("Indexed user \${user.username} into Elasticsearch")
        } catch (e: Exception) {
            logger.error("Failed to index user \${user.username}", e)
        }
    }

    fun searchUsers(query: String, limit: Int = 20): List<UserIndexDto> {
        if (client == null) return emptyList()

        return try {
            val req = SearchRequest.of { b ->
                b.index("users")
                 .query { q ->
                     q.bool { bool ->
                         bool.should { s ->
                             s.match { m -> m.field("username").query(query).fuzziness("AUTO") }
                         }.should { s ->
                             s.match { m -> m.field("fullName").query(query).fuzziness("AUTO") }
                         }
                     }
                 }
                 .size(limit)
            }
            
            val response = client.search(req, UserIndexDto::class.java)
            response.hits().hits().mapNotNull { it.source() }
        } catch (e: Exception) {
            logger.error("Elasticsearch query failed for \$query", e)
            emptyList()
        }
    }
}
