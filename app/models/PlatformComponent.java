package models;

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Lob;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Daniel Seybold on 23.11.2016.
 */
@Entity public class PlatformComponent extends Component {

    @Nullable
    @Lob
    private String gitUrl;

    @Nullable
    @Lob
    private String artifactPath;

    /**
     * Empty constructor for hibernate.
     */
    protected PlatformComponent(){

    }

    public PlatformComponent(@Nullable String gitUrl, @Nullable String artifactPath ){

        if(gitUrl == null){
            checkNotNull(artifactPath);
            checkArgument(!artifactPath.isEmpty());
        }

        if(artifactPath == null){
            checkNotNull(gitUrl);
            checkArgument(!gitUrl.isEmpty());
        }

        this.gitUrl = gitUrl;
        this.artifactPath = artifactPath;

    }
}
