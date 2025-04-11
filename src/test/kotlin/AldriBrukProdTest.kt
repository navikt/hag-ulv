import no.nav.helsearbeidsgiver.giBrukerAdvarselBrukDev
import no.nav.helsearbeidsgiver.kubernetes.KUBE_CTL_CONTEXT_ER_ALLTID_DEV
import org.junit.Test

class AldriBrukProdTest {
    @Test
    fun `Aldri bruk prod som kube ctl context`() {
        assert(KUBE_CTL_CONTEXT_ER_ALLTID_DEV == "dev-gcp") {
            "Feil kontekst: forventet 'dev-gcp', men fikk '$KUBE_CTL_CONTEXT_ER_ALLTID_DEV'"
        }
    }

    @Test
    fun `Gi bruker advarsel om context ikke er dev`() {
        if (KUBE_CTL_CONTEXT_ER_ALLTID_DEV != "dev-gcp") {
            giBrukerAdvarselBrukDev()
        }
    }
}
