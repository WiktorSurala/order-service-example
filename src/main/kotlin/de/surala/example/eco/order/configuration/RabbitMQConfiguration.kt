package de.surala.example.eco.order.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .findAndRegisterModules()

    }

    @Bean
    fun jsonMessageConverter(objectMapper: ObjectMapper): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }


    @Bean
    fun orderExchange(): TopicExchange {
        return TopicExchange("order.exchange")
    }

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: Jackson2JsonMessageConverter
    ): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        return template
    }
}
