package com.ukora.tradestudent.services.associations

import com.ukora.domain.entities.Brain
import com.ukora.tradestudent.services.BytesFetcherService
import spock.lang.Specification
import spock.lang.Unroll

class CaptureAssociationsServiceSpec extends Specification {

    CaptureAssociationsService captureAssociationsService = new CaptureAssociationsService()

    BytesFetcherService bytesFetcherService = Mock(BytesFetcherService)

    def setup() {
        captureAssociationsService.bytesFetcherService = bytesFetcherService
    }

    @Unroll
    def "Test CaptureNewValue with a variety of combined values"() {

        setup:
        Brain brain = new Brain(
                mean: mean as double,
                standard_deviation: standard_deviation as double,
                count: count
        )

        when:
        captureAssociationsService.captureNewValue(brain, value as double)

        then:
        1 * bytesFetcherService.saveBrain(_ as Brain) >> { List args ->
            final Brain b = args.first() as Brain
            assert b.count == expected_new_count
            assert b.mean == expected_new_mean
            assert b.standard_deviation == expexted_new_deviation
        }

        where:
        mean                        | standard_deviation           | count     | value                       | expected_new_count | expected_new_mean      | expexted_new_deviation
        0.00002555432165464654      | 0.000000023216551684851355   | 12315     | 0.000025448462321651D       | 12316              | 2.5554313059377544E-5D | 2.3235195777636377E-8D
        "0.00002555432165464654"    | "0.000000023216551684851355" | 12315 + 1 | 0.000025448462321651D       | 12316 + 1          | 2.555431306007538E-5D  | 2.3235194264684854E-8D
        5                           | 0.0                          | 0         | 5                           | 1                  | 5                      | 0D
        1.0407202987930506          | 0.0                          | 5         | 1.0507202987930506          | 6                  | 1.0423869654597173     | 0.003779644730092248D
        1.0407202987930506          | 0.1                          | 5         | 1.0407202987930506          | 6                  | 1.0407202987930508     | 0.09128709291752769D
        1.0407202987930506          | 0.0000001                    | 5         | 1.0407202987930506          | 6                  | 1.0407202987930508     | 9.128709291752768E-8D
        52                          | 0.0000000000                 | 5         | 52                          | 6                  | 52                     | 0.0D
        1                           | 0.0000000000                 | 5         | 1                           | 6                  | 1                      | 0.0D
        1.0407202987930506D         | 0                            | 5         | 1.0507202987930506D         | 6                  | 1.0423869654597173     | 0.003779644730092248D
        1.0407202987930506D         | 0.005                        | 5         | 1.0407202987930506D         | 6                  | 1.0407202987930508     | 0.004564354645876384D
        1.0407202987930506D         | 0                            | 5         | 1.0407202987930506D         | 6                  | 1.0407202987930508     | 0.0D
        1.0407202987930506 as float | 0                            | 5         | 1.0407202987930506 as float | 6                  | 1.0407203435897827     | 0.0D
        1.040720D                   | 0                            | 5         | 1.040720D                   | 6                  | 1.040720D              | 0.0D
        1.040720D                   | 0                            | 50        | 1.040720D                   | 51                 | 1.040720D              | 0.0D

    }
}
