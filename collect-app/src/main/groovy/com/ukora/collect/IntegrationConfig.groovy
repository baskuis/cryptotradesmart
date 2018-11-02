package com.ukora.collect

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IntegrationConfig {

    @Bean
    static SourceIntegration.KrakenIntegration getKrakenIntegration(){
        new SourceIntegration.KrakenIntegration()
    }

    @Bean
    static SourceIntegration.BinanceIntegration getBinanceIntegration(){
        new SourceIntegration.BinanceIntegration()
    }

    @Bean
    static SourceIntegration.CoinbaseProIntegration getCoinbaseProIntegration(){
        new SourceIntegration.CoinbaseProIntegration()
    }

    @Bean
    static SourceIntegration.BitstampIntegration getBitstampIntegration(){
        new SourceIntegration.BitstampIntegration()
    }

    @Bean
    static SourceIntegration.BitfinexIntegration getBitfinexIntegration(){
        new SourceIntegration.BitfinexIntegration()
    }

}
