package com.ukora.domain.entities

import org.springframework.data.annotation.Id


class Memory {

    @Id
    String id

    Exchange exchange
    Graph graph
    Bid bid
    Normalized normalized
    Ask ask
    Metadata metadata
}
